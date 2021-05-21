package implementation;

import baseclasses.FunctionalUnitBase;
import baseclasses.InstructionBase;
import baseclasses.Latch;
import baseclasses.PipelineStageBase;
import tools.MultiStageDelayUnit;
import tools.MyALU;
import utilitytypes.IFunctionalUnit;
import utilitytypes.IModule;

public class FloatMul extends FunctionalUnitBase {

	int wait;
	public FloatMul(IModule parent, String name, int wait)
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
			InstructionBase ins = input.getInstruction();
			if(input.isNull()) return;
			doPostedForwarding(input);
			
			
			int oper0 = ins.getOper0().getValue();
			float source1 = ins.getSrc1().getFloatValue();
			float source2 = ins.getSrc2().getFloatValue(); 
			
			
			int srce2 = Float.floatToRawIntBits(source2);
			int srce1 = Float.floatToRawIntBits(source1);
			
			
			int int_srceresult = MyALU.execute(ins.getOpcode(), srce1, srce2, oper0);
			
			float result = Float.intBitsToFloat(int_srceresult);
			output.setInstruction(ins);
			output.setResultFloatValue(result);
			
			 
		}
	}
	@Override
	public void createConnections()
	{
		addRegAlias("Delay.out","out");
		connect("in","FMulToDelay","Delay");
	}
	@Override
	public void createPipelineStages()
	{
		addPipeStage(new MyMathUnit(this));
	}
	@Override
	public void specifyForwardingSources()
	{
		addForwardingSource("out");
	}
	
	
	
	@Override
	public void createChildModules()
	{
		IFunctionalUnit child = new MultiStageDelayUnit(this,"Delay",this.wait);
		addChildUnit(child);
	}
	
	@Override
	public void createPipelineRegisters()
	{
		createPipeReg("FMulToDelay");
	}
	
}
