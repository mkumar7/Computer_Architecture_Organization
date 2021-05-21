
package implementation;

import baseclasses.PipelineRegister;
import baseclasses.PipelineStageBase;
import baseclasses.CpuCore;
import tools.InstructionSequence;
import voidtypes.VoidRegister;

public class MyCpuCore extends CpuCore<GlobalData> {
    PipelineRegister FetchToDecode;
    PipelineRegister DecodeToExecute;
    PipelineRegister ExecuteToMemory;
    PipelineRegister MemoryToWriteback;

    AllMyStages.Fetch       Fetch;
    AllMyStages.Decode      Decode;
    AllMyStages.Execute     Execute;
    AllMyStages.Memory      Memory;
    AllMyStages.Writeback   Writeback;

    public int count;

    private void setup() throws Exception {

        FetchToDecode     = new PipelineRegister(AllMyLatches.FetchToDecode.class);
        DecodeToExecute   = new PipelineRegister(AllMyLatches.DecodeToExecute.class);
        ExecuteToMemory   = new PipelineRegister(AllMyLatches.ExecuteToMemory.class);
        MemoryToWriteback = new PipelineRegister(AllMyLatches.MemoryToWriteback.class);


        registers.add(FetchToDecode);
        registers.add(DecodeToExecute);
        registers.add(ExecuteToMemory);
        registers.add(MemoryToWriteback);


        Fetch       = new AllMyStages.Fetch(this, VoidRegister.getVoidRegister(), FetchToDecode);
        Decode      = new AllMyStages.Decode(this, FetchToDecode, DecodeToExecute);
        Execute     = new AllMyStages.Execute(this, DecodeToExecute, ExecuteToMemory);
        Memory      = new AllMyStages.Memory(this, ExecuteToMemory, MemoryToWriteback);
        Writeback   = new AllMyStages.Writeback(this, MemoryToWriteback, VoidRegister.getVoidRegister());


        stages.add(Fetch);
        stages.add(Decode);
        stages.add(Execute);
        stages.add(Memory);
        stages.add(Writeback);

        globals = new GlobalData();
    }

    public MyCpuCore() throws Exception {
        setup();
    }

    public void loadProgram(InstructionSequence program) {
        globals.program = program;
    }

    public void runProgram() {

        globals.running = true;
        while (globals.running) {

            count++;
            advanceClock();
            System.out.println(count);


        }
    }
}
