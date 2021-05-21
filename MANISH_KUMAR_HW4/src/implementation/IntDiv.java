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
public class IntDiv extends PipelineStageBase {
    int count = 0;
    
    public IntDiv(IModule parent) {
        super(parent, "IntDiv");
    }
    
    
    @Override
    public void compute(Latch input, Latch output) {
        while (input.isNull()) return; 
        doPostedForwarding(input); 
        
        while (count < 15)
        {
            count++;
            this.setResourceWait("Loop"+ count);
            return;
        }
         count = 0;

        InstructionBase ins = input.getInstruction();
        int result = 0;
        int source1 = ins.getSrc1().getValue();
        int source2 = ins.getSrc2().getValue();

       
        switch (ins.getOpcode()) {
        case MOD:
            result = source1 % source2;
            break;    
        case DIV:
                result = source1 / source2;
                break;
           
        }
        output.setResultValue(result);
        output.setInstruction(ins);
        
    }
    
}
