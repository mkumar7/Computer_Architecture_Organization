/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import baseclasses.InstructionBase;
import baseclasses.Latch;
import baseclasses.PipelineStageBase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import utilitytypes.EnumOpcode;
import utilitytypes.ICpuCore;
import utilitytypes.IModule;
import utilitytypes.IPipeReg;
import utilitytypes.Logger;
import utilitytypes.Operand;

/**
 * Some other students had the idea to store the Latch object containing a 
 * dispatched instruction into the IQ.  This allowed them to use the pre-
 * existing doForwardingSearch() method to scan for completing inputs for
 * instructions.  I consider that to be an excellent alternative approach
 * to what I did here.
 * 
 * @author millerti
 */
public class IssueQueue extends PipelineStageBase 
{
    
    public IssueQueue(IModule parent) 
    {
        super(parent, "IssueQueue");
    }
    
    static private class Completion 
    {
        String register_name;
        int val;
        boolean later, is_float;
        Completion(String rname, int val, boolean m, boolean k) 
        {
        	
            register_name = rname;
             later = m;
            is_float = k;
            val = val;
        }
        public String valueToString() 
        {
            return is_float ? Float.toString(Float.intBitsToFloat(val)) : Integer.toString(val);
        }
    }
    
    static private class IQEntry 
    {
    	
              Operand[] operand;
              InstructionBase ins;

              int optport;
        IQEntry(InstructionBase ins, Operand[] ops, int outputport)
        {
        	
        	 operand = ops;
            this.ins = ins;
           
            optport = outputport;
        }
    }
    
    private int opcodeOutputPort(EnumOpcode op) 
    {
        int out_number;
       
        if (op == EnumOpcode.FADD || op == EnumOpcode.FSUB || op == EnumOpcode.FCMP) 
        {
            out_number = lookupOutput("IQToFloatAddSub");
        } 
        else if (op == EnumOpcode.FMUL) 
        {
            out_number = lookupOutput("IQToFloatMul");
        } 
        else if (op == EnumOpcode.FDIV) 
        {
            out_number = lookupOutput("IQToFloatDiv");
        } 
       else if (op == EnumOpcode.MUL) 
        {
            out_number = lookupOutput("IQToIntMul");
        } 
        else if (op == EnumOpcode.DIV || op == EnumOpcode.MOD) 
        {
            out_number = lookupOutput("IQToIntDiv");
        }
        else if (op.accessesMemory()) 
             {
                 out_number = lookupOutput("IQToMemory");
             } else 
             {
                 out_number = lookupOutput("IQToExecute");
             }
        return out_number;
    }
    
    Map<Integer,Completion> complete_now  = null;
    Map<Integer,Completion> complete_fur = null;
    IQEntry iq[] = new IQEntry[512];
    Map<Integer,Completion> complete_previous = new HashMap<>();
    
    int entries = 0;
    int use_entries = 0;
    @Override
    public void compute() 
    {
    	 Latch in = readInput(0);
        Map<Integer,String> doing = new HashMap<>();
        int ient = -1;
        ICpuCore core = getCore();

       
        InstructionBase ins = in.getInstruction();
        
        if (!ins.isNull())
        {
            int k = 0;
            do
            {
           
            
                if (iq[use_entries] == null) 
                {
                    ient = use_entries;
                    break;
                }
                use_entries++;
                use_entries &= 511;
                
                k++;
            }
            while(k<512);
            
            if (ient>=0) 
            {
            	 Operand[] ops = {ins.getOper0(), ins.getSrc1(), ins.getSrc2()};  

                for (int j=0; j<3; j++) 
                {
                    if (ops[j] == null) continue;
                    if (!ops[j].isRegister()) ops[j] = null;
                  
                }
                int outport = opcodeOutputPort(ins.getOpcode());
            	use_entries++;
                core.incDispatched();
                doing.put(ient, ins.toString() + " [new]");

               
                if (!ins.getOpcode().oper0IsSource()) ops[0] = null;
           
                
               
                iq[ient] = new IQEntry(in.getInstruction(), ops, outport);
                in.consume();
            } 
            else 
            {
                addStatusWord("IQ Full");
            }
        }
        
        

        Set<String> fwdsrcs = core.getForwardingSources();
        
       
        complete_now  = new HashMap<>();
        complete_fur = new HashMap<>();
        for (String fsrc : fwdsrcs) 
        {
        	 IPipeReg pipe_register = core.getPipeReg(fsrc);
        	Latch latch = pipe_register.read();
        	 int forward_register = latch.getResultRegNum();
           
            if (!latch.isNull() && latch.hasResultValue()) 
            {
               
                if (forward_register >= 0) 
                {
                    int value = latch.getResultValue();
                    boolean isfloat = latch.isResultFloat();
                    complete_now.put(forward_register, new Completion(fsrc, value, false, isfloat));
                }
            }
            
            Latch next = pipe_register.readNextCycle();
          
            
        }
        

        int count = 0;
       
    
        for (int k=0; k<256; k++) 
    
        {
            IQEntry ie = iq[k];
            if (ie == null) continue;
            count++;
            

            Operand[] operands = ie.operand;
            int m =0;
            do
            {
           
            
                Operand op = operands[m];
                if (op != null && op.isRegister() && !op.hasValue()) 
                {
                    int register_number = op.getRegisterNumber();
                    Completion equal1 = complete_now.get(register_number);
                    Completion equal2 = complete_previous.get(register_number);
                    
                    if (equal1 != null) 
                    {
                        op.setValue(equal1.val, equal1.is_float);
                        Logger.out.println("# Forward from " + equal1.register_name + " this cycle to IQ: op" + m + " of " + ie.ins);
                    }
                    if (equal1 != null && equal2 != null) 
                    {
                        throw new RuntimeException("Two completions to the same physreg on consecutive cycles");
                    }
                   
                    if (equal2 != null) 
                    {
                        op.setValue(equal2.val, equal2.is_float);
                        Logger.out.println("# Forward from " + equal1.register_name + " prev cycle to IQ: op" + m + " of " + ie.ins);
                    }
                }
                m++;
            }
            while(m<3);
           
        }
        
        
       
        complete_previous = complete_now;
        
        
       
        int num_output = numOutputRegisters();
        boolean[] Work = new boolean[num_output];
        int j =0;
        do
        	
       
        {
            Work[j] = outputCanAcceptWork(j);
            j++;
        }
        while(j<num_output);
        
      
        Latch[] select = new Latch[num_output];
        int[] index = new int[num_output];
        String[] forward_source = new String[3];
        int k = 0;
        
        for (int i=0; i<256; i++) 
        	{
        	
            IQEntry ie = iq[k];
            if (ie == null) continue;
            int port = ie.optport;
           
           
            if (!Work[port] || select[port] != null)
            {
            	
                while (!doing.containsKey(k)) 
                {
                    doing.put(k, ie.ins.toString());
                }
                continue;
            }
            

            
            Operand[] oper = ie.operand;
            boolean f = false;
            int n = 0;
            do
            {
            
                forward_source[n] = null;
                Operand op = oper[n];
                if (op != null && !op.hasValue()) 
                {
                    int register_number = op.getRegisterNumber();

                    Completion match = complete_fur.get(register_number);
                     if (match == null) 
                    {
                        f = true;
                        break;
                    } 
                    else 
                    {
                        forward_source[n] = match.register_name;
                    }
                }
                n++;
            }
            while(n<3);
            if (f) 
            {
            	
                while (!doing.containsKey(k)) 
                {
                    doing.put(k, ie.ins.toString());
                   
                }
                
                continue;
            }
            
            Latch output = this.newOutput(port);
            String next_stage = output.getParentRegister().getStageAfter().getName();

            if (doing.containsKey(k)) 
            {
                doing.put(k, doing.get(k) + " [selected]");
            } 
            else 
            {
                doing.put(k, ie.ins.toString() + " [selected]");
            }
            output.setInstruction(ie.ins);
            int m =0;
            
            do
            {
            	
                while (forward_source[m] != null) 
                {
                    output.setProperty("forward"+m, forward_source[m]);
                    Logger.out.println("# Posting forward from " + forward_source[m] + " next cycle to " + next_stage + ": op" + m + " of " + ie.ins);
                }
                m++;
            }
            while(m<3);
            select[port] = output;
            index[port] = k;
            
        }
        
       
        for (int n=0; n<num_output; n++) 
        {
        	
            if (select[n] != null) 
            {
                use_entries--;
                core.incIssued();
                select[n].write();
                iq[index[n]] = null;
            }
            
        }
       
        
        setActivity(String.join("\n", doing.values()));
    }

    
}
