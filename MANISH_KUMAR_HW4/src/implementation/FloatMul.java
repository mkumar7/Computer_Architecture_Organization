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
import utilitytypes.IFunctionalUnit;
import utilitytypes.IModule;

/**
 *
 * @author millerti
 */
public class FloatMul extends FunctionalUnitBase 
{

    public FloatMul(IModule parent) 
    {
        super(parent, "FloatMul");
    }
    
    private static class FMul extends PipelineStageBase
    {
        public FMul(IModule parent) {
            super(parent, "in"); 
        }
        
        @Override
        public void compute(Latch input, Latch output)
        {
        	doPostedForwarding(input);
            while (input.isNull()) return;
            
            InstructionBase ins = input.getInstruction();

            float source1 = ins.getSrc1().getFloatValue();
            float source2 = ins.getSrc2().getFloatValue();

            float result2 = source1 * source2;
            output.setResultFloatValue(result2);
            output.setInstruction(ins);
            
        }
    }
    
    @Override
    public void createConnections() 
    {
        connect("in", "FMulToDelay", "Delay");
    }
    
    @Override
    public void createPipelineStages() 
    {
        addPipeStage(new FMul(this));
    }
    @Override
    public void specifyForwardingSources() 
    {
        addForwardingSource("out");
    }
    
    @Override
    public void createChildModules() 
    {
        IFunctionalUnit child = new MultiStageDelayUnit(this, "Delay", 5);
        addChildUnit(child);
        this.addRegAlias("Delay.out", "out");
    }

    @Override
    public void createPipelineRegisters()
    {
        createPipeReg("FMulToDelay");
    }

   

   

   
   
}
