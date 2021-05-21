package implementation;

import utilitytypes.EnumOpcode;

public class MyALU {
    static int execute(EnumOpcode opcode, int input1, int input2, int oper0) {
        int result = 0;

        switch (opcode) {


            case SHL:
                result = input1 << input2;
                break;
            case AND:
                result = input1 & input2;
                break;
            case XOR:
                result = input1 ^ input2;
                break;
            case ADD:
                result = input1 + input2;
                break;

            case SUB:
                result = input1 - input2;
                break;
            case OR:
                result = input1 | input2;
                break;
            case ASR:
                result = input1 >> input2;
                break;

            case LOAD:
            case STORE:
                result = input1 + input2;
                break;

            case LSR:
                result = input1 >>> input2;
                break;

            case MOVC:
                result = input1;
                break;

            case CMP:
                result = input1 - input2;
                break;

            case OUT:

                System.out.println("@@output: " + oper0);
                break;
        }

        return result;
    }
}