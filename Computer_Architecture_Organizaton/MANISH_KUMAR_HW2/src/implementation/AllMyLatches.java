package implementation;

import java.util.ArrayList;
import java.util.List;

import baseclasses.InstructionBase;

import baseclasses.LatchBase;
import baseclasses.PipelineRegister;
import utilitytypes.EnumOpcode;


public class AllMyLatches {
    public static class FetchToDecode extends LatchBase {

    }

    public static class DecodeToExecute extends LatchBase {

        @override
        public boolean isForwardingResultValid() {
            return false;
        }
        @override
        public boolean isForwardingResultValidNextCycle() {
            InstructionBase in = getInstruction();
            switch(in.getOpcode())
            {
                case LOAD:
                    return false;
                case STORE:
                    return false;

                default:
                    return true;
            }
        }

    }



    public static class ExecuteToMemory extends LatchBase {





        public int result;
        InstructionBase in = getInstruction();
        @override
        public boolean isForwardingResultValid() {

            switch(in.getOpcode())
            {
                case STORE:
                    return false;
                case LOAD:
                    return false;

                default:
                    return true;
            }


        }




        @override
        public boolean isForwardingResultValidNextCycle() {

            switch(in.getOpcode())
            {
                case STORE:
                    return false;
                default:
                    return true;
            }
        }

        @override
        public int getForwardingResultValue() {
            int regnum = this.getForwardingDestinationRegisterNumber();
            if(regnum>0)
            {
                return result;
            }
            else
            {
                return 0;
            }

        }
    }

    public static class MemoryToWriteback extends LatchBase {

        public int result;
        @override
        public boolean isForwardingResultValid() {

            InstructionBase in = getInstruction();
            switch(in.getOpcode())
            {
                case STORE:
                    return false;
                default:
                    return true;
            }

        }


    }
}
