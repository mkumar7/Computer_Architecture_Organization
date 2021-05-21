/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import baseclasses.FunctionalUnitBase;
import baseclasses.InstructionBase;
import baseclasses.Latch;
import baseclasses.PipelineStageBase;
import tools.MultiStageDelayUnit;
import utilitytypes.ICpuCore;
import utilitytypes.IFunctionalUnit;
import utilitytypes.IGlobals;
import utilitytypes.IModule;
import static utilitytypes.IProperties.MAIN_MEMORY;
import utilitytypes.Operand;

/**
 *
 * @author millerti
 */
public class MemUnit extends FunctionalUnitBase 
{

    public MemUnit(IModule parent)
    {
        super(parent, "MemUnit");
    }

    
    private static class Address extends PipelineStageBase
    {
        public Address(IModule parent)
        {
            super(parent, "in:Addr");
        }
        
        @Override
        public void compute(Latch input, Latch output)
        { 
        	doPostedForwarding(input);
            while (input.isNull()) return;
            
            InstructionBase ins = input.getInstruction();

            int src1 = ins.getSrc1().getValue();
            int src2 = ins.getSrc2().getValue();

            int addr = src1 + src2;
            
            output.setInstruction(ins);           
            output.setProperty("address", addr);
            
        }
    }
    
    private static class LSQ extends PipelineStageBase
    {
        public LSQ(IModule parent)
        {
            super(parent, "LSQ");
        }
        
        @Override
        public void compute(Latch input, Latch output)
        {
            while (input.isNull()) return;
            this.addStatusWord("Addr=" + input.getPropertyInteger("address"));
            output.copyAllPropertiesFrom(input);
            output.setInstruction(input.getInstruction());
            
        }
    }
    
    static class DCache extends PipelineStageBase
    {
        public DCache(IModule parent)
        {
            super(parent, "DCache");
        }

        @Override
        public void compute(Latch input, Latch output)
        {
            while(input.isNull()) return;
            InstructionBase ins = input.getInstruction();

            IGlobals globals = (GlobalData)getCore().getGlobals();
            int value = 0;
          
            int oper0val = ins.getOper0().getValue();
            int addr = input.getPropertyInteger("address");
            int[] memory = globals.getPropertyIntArray(MAIN_MEMORY);
            Operand oper0 = ins.getOper0();
           
           

            switch (ins.getOpcode())
            {
                
            case STORE:
                // For store, the value to be stored in main memory is
                // in oper0, which was fetched in Decode.
                memory[addr] = oper0val;
                addStatusWord("Mem[" + addr + "]=" + oper0.getValueAsString());
                return;
            
            case LOAD:
                    // Fetch the value from main memory at the address
                    // retrieved above.
            	 addStatusWord(oper0.getRegisterName() + "=Mem[" + addr + "]");
                    output.setInstruction(ins);
                    output.setResultValue(value);
                    value = memory[addr];
                   
                   
                    break;
                
              
                    
                default:
                    throw new RuntimeException("Non-memory instruction got into Memory stage");
            }
        }
    }
    
    @Override
    public void createConnections() 
    {
    	 connect("DCache", "out");
        connect("LSQ", "LsqToDcache", "DCache");
       
        connect("in:Addr", "AddrToLSQ", "LSQ");
    }

    @Override
    public void createPipelineRegisters()
    {
    	 createPipeReg("out");
        createPipeReg("LsqToDcache");
       
        createPipeReg("AddrToLSQ");
    }
    @Override
    public void specifyForwardingSources()
    {
        addForwardingSource("out");
    }   

    @Override
    public void createPipelineStages() 
    {
    	 addPipeStage(new DCache(this));
        addPipeStage(new LSQ(this));
       
        addPipeStage(new Address(this));
    }

    @Override
    public void createChildModules() 
    {
    }

   
    
}
