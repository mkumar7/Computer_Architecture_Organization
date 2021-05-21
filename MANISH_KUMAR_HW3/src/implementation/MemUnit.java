package implementation;

import static utilitytypes.IProperties.MAIN_MEMORY;

import baseclasses.FunctionalUnitBase;
import baseclasses.InstructionBase;
import baseclasses.Latch;
import baseclasses.PipelineStageBase;
import utilitytypes.IGlobals;
import utilitytypes.IModule;
import utilitytypes.Operand;



public class MemUnit extends FunctionalUnitBase{
     int wait;
     public MemUnit(IModule parent,String name,int wait)
     {
    	 super(parent,name);
    	 this.wait = wait; 
     }
     
     
     private class Addr extends PipelineStageBase
     {
    	 public Addr(IModule parent)
    	 {
    		 super(parent,"in:Addr");
    	 }
    	 
    	 @Override
    	 public void compute(Latch input, Latch output)
    	 {
    		 InstructionBase ins = input.getInstruction();
    		 if(input.isNull()) return;
    		 doPostedForwarding(input);
    		 
    		 output.copyAllPropertiesFrom(input);
    		 output.setInstruction(input.getInstruction());
    		 
    	 }
    	 
    	 
     }
     
     private class LSQ extends PipelineStageBase                   
     {
    	public LSQ(IModule parent)
    	{
    		super(parent,"LSQ");
    	}
    	
    	@Override 
    	public void compute(Latch input,Latch output)
    	{
    		InstructionBase ins = input.getInstruction();
    		doPostedForwarding(input);
    		if(input.isNull()) return;
    		
    		
    		setActivity(ins.toString());
    		
    		Operand oper0 = ins.getOper0();
    		int oper0val = ins.getOper0().getValue();
    		int source1 = ins.getSrc1().getValue(); 
    		int source2 = ins.getSrc2().getValue(); 
    		
    		int addr = source1 + source2;
    		
    		addStatusWord("Addr =" + addr);
    		output.copyAllPropertiesFrom(input);
    		output.setInstruction(input.getInstruction());
    		
    	}
     }
     
     private class DCache extends PipelineStageBase
     {
    
		 


		public DCache(IModule parent)
    	 {
    		 super(parent,"DCache");
    	 }
    	 
    	 
    	 @Override
    	 public void compute(Latch input,Latch output)
    	 {
    		 InstructionBase ins = input.getInstruction();
    		 if(input.isNull()) return;
    		 
    		setActivity(ins.toString());
    		 doPostedForwarding(input);
    		 
    		 
    		 Operand oper0 = ins.getOper0();
    		 int oper0val = ins.getOper0().getValue();
    		 int source1 = ins.getSrc1().getValue();
    		 int source2 = ins.getSrc2().getValue();
    		 
    		/* int addr = source1 + source2;
    		 
    		 int value=0;
    		 
    		IGlobals globals = (GlobalData)getCore().getGlobals(); 
			int[] memory = globals.getPropertyIntArray(globals.MAIN_MEMORY);*/
    		 
    		 
             
             int value = 0;
             IGlobals globals = (GlobalData)getCore().getGlobals();
             int[] memory = globals.getPropertyIntArray(MAIN_MEMORY); 
			
             int addr = source1 + source2;
    		 switch(ins.getOpcode())
    		 {
    		 case STORE:
    			 memory[addr] = oper0val;
    			 addStatusWord("Mem[" + addr + "]="+ ins.getOper0().getValueAsString());
    			 break;
    			 
    			 default:
    			 throw new RuntimeException("Non-memory instruction got into Memory stage"); 
    		 
    		 case LOAD:
    			 value = memory[addr]; 
    			 addStatusWord("Mem["+addr+"]");
    			 output.setInstruction(ins);
    			 output.setResultValue(value);
    			
    			 break;
    			 
    		
    		 }
    		 
    		 
    	 }
     }                   
     
     
         
     @Override
     public void createConnections()
     {
    	 connect("DCache","out");
    	 connect("LSQ","LsqToDCache","DCache");
    	 connect("in:Addr","AddrToLSQ","LSQ");
     }
     
    
     @Override

     public void createPipelineRegisters()
     {
    	 createPipeReg("out");
    	 createPipeReg("LsqToDCache");
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
    	 addPipeStage(new Addr(this));
     }
     @Override
     public void createChildModules()
     {
    	 
     }
     
   
     
     
 
}
