package implementation;

import baseclasses.FunctionalUnitBase;
import baseclasses.InstructionBase;
import baseclasses.Latch;
import baseclasses.PipelineStageBase;
import tools.MultiStageDelayUnit;
import tools.MyALU;
import utilitytypes.IFunctionalUnit;
import utilitytypes.IModule;

public class IntMul extends FunctionalUnitBase {
    
	int wait;
	public IntMul(IModule parent, String name, int wait)
	{
		super(parent,name);
		this.wait = wait;
	}
	
	private static class MyMathUnit extends PipelineStageBase
	{
		public MyMathUnit(IModule parent)
		{
			super(parent,"in");
		}
		
		
		@Override
		public void compute(Latch input, Latch output) 
		{
			
			if(input.isNull()) return;
			InstructionBase ins = input.getInstruction();
			doPostedForwarding(input);
			
			
			
			int oper0 = ins.getOper0().getValue();
			
			int source2 = ins.getSrc2().getValue();
			int source1 = ins.getSrc1().getValue();
			int result = MyALU.execute(ins.getOpcode(), source1, source2, oper0);
			output.setInstruction(ins);
			output.setResultValue(result);
			
			
		} 
	}
	
	@Override
	public void createChildModules()
	{
		IFunctionalUnit child = new MultiStageDelayUnit(this,"Delay",this.wait);
		addChildUnit(child);
	}
	
	@Override
	public void specifyForwardingSources()
	{
		addForwardingSource("out");
	}
	@Override
	public void createConnections()
	{
		addRegAlias("Delay.out","out"); 
		connect("in","MulToDelay","Delay");
	}
	@Override
	public void createPipelineRegisters()
	{
		createPipeReg("MulToDelay");
	}
	
	
	
	@Override
	public void createPipelineStages()
	{
		addPipeStage(new MyMathUnit(this));
	}
	
}
