package implementation;

import baseclasses.InstructionBase;
import baseclasses.Latch;
import baseclasses.PipelineStageBase;
import tools.MyALU;
import utilitytypes.ICpuCore;

public class FloatDiv extends PipelineStageBase {
	
	public FloatDiv(ICpuCore core)
	{
		super(core,"FloatDiv");
	}
	
	@Override
	public void compute(Latch input, Latch output)
	{ 
		if(input.isNull()) return;
		
		InstructionBase ins = input.getInstruction();
		
		GlobalData globals = (GlobalData)getCore().getGlobals();
		doPostedForwarding(input); 
		
		
		if(globals.getDivisionCount() < globals.DIVISION_NO)
		{
			globals.increaseDivisionCount(); 
			setResourceWait("Loop"+globals.getDivisionCount());
			
					
		}
		
		else
		{
			globals.resetDivisionCount();
		}
		int oper0 = ins.getOper0().getValue();
		float source1 = ins.getSrc1().getFloatValue();
		float source2 = ins.getSrc2().getFloatValue();
		
		
		int srce1 = Float.floatToRawIntBits(source1);
		int srce2 = Float.floatToRawIntBits(source2);
		
		int int_srceresult = MyALU.execute(ins.getOpcode(), srce1, srce2, oper0);
		
		float result = Float.intBitsToFloat(int_srceresult);
		output.setInstruction(ins);
		output.setResultFloatValue(result);
		
		
	}

}
