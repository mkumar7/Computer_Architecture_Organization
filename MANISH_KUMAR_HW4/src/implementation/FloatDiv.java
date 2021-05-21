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
import utilitytypes.IModule;

/**
 *
 * @author millerti
 */
public class FloatDiv extends PipelineStageBase
{
    int count = 0;
    
    public FloatDiv(IModule parent)
    {
        super(parent, "FloatDiv"); 
    }
    
    
    @Override
    public void compute(Latch input, Latch output)
    {
        while (input.isNull()) return;
        doPostedForwarding(input);
        
        while (count < 15) 
        {
            count++;
            this.setResourceWait("Loop"+count);
            return;
        }
         count = 0;

        InstructionBase ins = input.getInstruction();
        
        float src1 = ins.getSrc1().getFloatValue();
        float src2 = ins.getSrc2().getFloatValue();

        float result3 = src1 / src2;
        output.setResultFloatValue(result3);
        output.setInstruction(ins);
        
    }
    
}
