/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import utilitytypes.EnumOpcode;
import utilitytypes.Logger;

/**
 * The code that implements the ALU has been separates out into a static
 * method in its own class.  However, this is just a design choice, and you
 * are not required to do this.
 * 
 * @author 
 */
public class MyALU {
    static public int execute(EnumOpcode opcode, int input1, int input2, int oper0) {
        int result = 0;
        float source1;
        float source2;
        float float_result;
        
        switch (opcode) {
        
        case FSUB:
        	source1 = Float.intBitsToFloat(input1);
        	source2 = Float.intBitsToFloat(input2);
        	float_result = source1 - source2;
        	result = Float.floatToIntBits(float_result);
        	break; 
        case FMUL:
        	source1 = Float.intBitsToFloat(input1);
        	source2 = Float.intBitsToFloat(input2);
        	float_result = source1 * source2;
        	result = Float.floatToIntBits(float_result); 
        	break;
            
        case FADD:
        	source1 = Float.intBitsToFloat(input1);
        	source2 = Float.intBitsToFloat(input2);
        	float_result = source1 + source2;
        	result = Float.floatToIntBits(float_result);
        	break;
        case FDIV:
        	source1 = Float.intBitsToFloat(input1);
        	source2 = Float.intBitsToFloat(input2);
        	float_result = source1 / source2;
        	result = Float.floatToIntBits(float_result);
        	break;
        case FCMP:
        	source1 = Float.intBitsToFloat(input1);
        	source2 = Float.intBitsToFloat(input2);
        	float_result = source1 - source2;
        	result = Float.floatToIntBits(float_result);
        	break;
            case CALL:
            case SHL:
                result = input1 << input2;
                break;
            case OR:
                result = input1 | input2;
                break;
                
            case SUB:
                result = input1 - input2;
                break;
                
            case DIV:
            	result = input1 / input2;
            	break;
                
            case LSR:
                result = input1 >>> input2;
                break;
                
            case CMP:
                // if a<b then a-b<0
                // if a>b then a-b>0
                // if a==b then a-b==0
                result = input1 - input2;
                break;
                
             case ADD:
                result = input1 + input2;
                break;
         
           
             	
            
            	
            case AND:
                result = input1 & input2;
                break;
            
            case XOR:
                result = input1 ^ input2;
                break;
            case MUL:
            	result = input1*input2;
            	break;
                
           
            case ASR:
                result = input1 >> input2;
                break; 
           
            case OUT:
                // It doesn't really matter which stage OUT is processed in.
                // I did it in Execute.  Some people did it in Writeback.
                // Since OUT is inspired by an I/O instruction in real 
                // CPUs, possibly the most "purist" stage to execute it in
                // would be Memory.
                Logger.out.println("@@output: " + oper0);
                break;
                
                
         
                
            case FOUT:
                Logger.out.println("@@output: " + Float.intBitsToFloat(oper0));
                break;
            case LOAD:
            case STORE:
                throw new RuntimeException("Load/Store got into Execute");
                
            case MOV:
                result = input1;
                break;
           
        }
        
        return result;
    }    
}
