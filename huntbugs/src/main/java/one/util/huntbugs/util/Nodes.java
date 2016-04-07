/*
 * Copyright 2015, 2016 Tagir Valeev
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package one.util.huntbugs.util;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import one.util.huntbugs.flow.ValuesFlow;

import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.ast.AstCode;
import com.strobel.decompiler.ast.Block;
import com.strobel.decompiler.ast.Expression;
import com.strobel.decompiler.ast.Node;
import com.strobel.decompiler.ast.TryCatchBlock;

/**
 * @author lan
 *
 */
public class Nodes {
    public static boolean isOp(Node node, AstCode op) {
        return node instanceof Expression && ((Expression)node).getCode() == op;
    }
    
    public static boolean isInvoke(Node node) {
        if(!(node instanceof Expression))
            return false;
        AstCode code = ((Expression)node).getCode();
        return code == AstCode.InvokeDynamic || code == AstCode.InvokeStatic || code == AstCode.InvokeSpecial
                || code == AstCode.InvokeVirtual || code == AstCode.InvokeInterface;
    }
    
    public static boolean isNullCheck(Node node) {
        if(!isOp(node, AstCode.CmpEq) && !isOp(node, AstCode.CmpNe))
            return false;
        List<Expression> args = ((Expression)node).getArguments();
        return args.get(0).getCode() == AstCode.AConstNull ^ args.get(1).getCode() == AstCode.AConstNull;
    }
    
    public static Node getChild(Node node, int i) {
        if(node instanceof Expression) {
            return ValuesFlow.getSource(((Expression)node).getArguments().get(i));
        }
        return node.getChildren().get(i);
    }
    
    public static Expression getChild(Expression node, int i) {
        return ValuesFlow.getSource(node.getArguments().get(i));
    }
    
    public static Object getConstant(Node node) {
        if(!(node instanceof Expression))
            return null;
		Expression expr = ValuesFlow.getSource((Expression) node);
        if(expr.getCode() != AstCode.LdC)
            return null;
        return expr.getOperand();
    }

    public static void ifBinaryWithConst(Expression expr, BiConsumer<Expression, Object> consumer) {
        if(expr.getArguments().size() == 2) {
            Expression left = expr.getArguments().get(0);
            Expression right = expr.getArguments().get(1);
            Object constant = getConstant(left);
            if(constant != null) {
                consumer.accept(right, constant);
            } else {
                constant = getConstant(right);
                if(constant != null) {
                    consumer.accept(left, constant);
                }
            }
        }
    }
    
    public static boolean isComparison(Node node) {
        if(!(node instanceof Expression) || node.getChildren().size() != 2)
            return false;
        switch (((Expression) node).getCode()) {
        case CmpEq:
        case CmpGe:
        case CmpGt:
        case CmpLe:
        case CmpLt:
        case CmpNe:
            return true;
        default:
            return false;
        }
    }

    public static boolean isBinaryMath(Node node) {
        if(!(node instanceof Expression) || node.getChildren().size() != 2)
            return false;
        switch (((Expression) node).getCode()) {
        case Add:
        case Sub:
        case Mul:
        case Div:
        case Rem:
        case Shl:
        case Shr:
        case UShr:
        case And:
        case Or:
        case Xor:
            return true;
        default:
            return false;
        }
    }
    
    public static boolean isBoxing(Node node) {
        if(!isOp(node, AstCode.InvokeStatic))
            return false;
        MethodReference ref = (MethodReference)((Expression)node).getOperand();
        if(!ref.getName().equals("valueOf"))
            return false;
        TypeReference type = ref.getDeclaringType();
        if(type.getInternalName().equals("java/lang/Double") && ref.getSignature().equals("(D)Ljava/lang/Double;"))
            return true;
        if(type.getInternalName().equals("java/lang/Integer") && ref.getSignature().equals("(I)Ljava/lang/Integer;"))
            return true;
        if(type.getInternalName().equals("java/lang/Long") && ref.getSignature().equals("(J)Ljava/lang/Long;"))
            return true;
        if(type.getInternalName().equals("java/lang/Boolean") && ref.getSignature().equals("(Z)Ljava/lang/Boolean;"))
            return true;
        if(type.getInternalName().equals("java/lang/Short") && ref.getSignature().equals("(S)Ljava/lang/Short;"))
            return true;
        if(type.getInternalName().equals("java/lang/Character") && ref.getSignature().equals("(C)Ljava/lang/Character;"))
            return true;
        if(type.getInternalName().equals("java/lang/Float") && ref.getSignature().equals("(F)Ljava/lang/Float;"))
            return true;
        if(type.getInternalName().equals("java/lang/Byte") && ref.getSignature().equals("(B)Ljava/lang/Byte;"))
            return true;
        return false;
    }

    public static boolean isUnboxing(Node node) {
        if(!isOp(node, AstCode.InvokeVirtual))
            return false;
        MethodReference ref = (MethodReference)((Expression)node).getOperand();
        TypeReference type = ref.getDeclaringType();
        if(type.getInternalName().equals("java/lang/Double") && ref.getName().equals("doubleValue"))
            return true;
        if(type.getInternalName().equals("java/lang/Integer") && ref.getName().equals("intValue"))
            return true;
        if(type.getInternalName().equals("java/lang/Long") && ref.getName().equals("longValue"))
            return true;
        if(type.getInternalName().equals("java/lang/Boolean") && ref.getName().equals("booleanValue"))
            return true;
        if(type.getInternalName().equals("java/lang/Short") && ref.getName().equals("shortValue"))
            return true;
        if(type.getInternalName().equals("java/lang/Character") && ref.getName().equals("charValue"))
            return true;
        if(type.getInternalName().equals("java/lang/Float") && ref.getName().equals("floatValue"))
            return true;
        if(type.getInternalName().equals("java/lang/Byte") && ref.getName().equals("byteValue"))
            return true;
        return false;
    }
    
    public static Expression getThis(Expression node) {
        if(node.getCode() == AstCode.GetField || node.getCode() == AstCode.PutField)
            return node.getArguments().get(0);
        if(node.getCode() == AstCode.GetStatic || node.getCode() == AstCode.PutStatic)
            return null;
        throw new IllegalArgumentException(node+": expected field operation");
    }

    public static boolean isEquivalent(Node expr1, Node expr2) {
        if(expr1 == expr2)
            return true;
        if(expr1 == null)
            return expr2 == null;
        if(expr1 instanceof Expression && expr2 instanceof Expression)
            return ((Expression)expr1).isEquivalentTo((Expression) expr2)
                    && isSideEffectFree(expr1);
        return false;
    }

	public static boolean isSideEffectFree(Node node) {
	    if(node == null)
	        return true;
	    if(!(node instanceof Expression))
	        return false;
	    Expression expr = (Expression)node;
		switch(expr.getCode()) {
	    case PreIncrement:
	    case PostIncrement:
	    case InvokeDynamic:
	    case Store:
	    case StoreElement:
	    case CompoundAssignment:
	    case PutField:
	    case PutStatic:
	    case InitArray:
	    case InitObject:
	        return false;
	    case InvokeSpecial:
	    case InvokeStatic:
	    case InvokeVirtual:
	        if(!isBoxing(node) && !isUnboxing(node))
	            return false;
	    default:
	        for(Expression child : expr.getArguments()) {
	            if(!isSideEffectFree(child))
	                return false;
	        }
	    }
		return true;
	}
	
	public static boolean isSynchorizedBlock(Node node) {
	    if(!(node instanceof TryCatchBlock)) {
	        return false;
	    }
	    Block finallyBlock = ((TryCatchBlock)node).getFinallyBlock();
	    if(finallyBlock == null)
	        return false;
	    return finallyBlock.getBody().stream().anyMatch(n -> Nodes.isOp(n, AstCode.MonitorExit));
	}
	
	public static boolean isCompoundAssignment(Node node) {
	    if(!(node instanceof Expression))
	        return false;
	    Expression store = (Expression) node;
	    if(store.getCode() != AstCode.Store)
	        return false;
	    Expression expr = store.getArguments().get(0);
	    if(!isBinaryMath(expr))
	        return false;
	    Expression load = expr.getArguments().get(0);
	    return load.getCode() == AstCode.Load && Objects.equals(load.getOperand(), store.getOperand());
	}
	
	public static Node find(Node node, Predicate<Node> predicate) {
	    if(predicate.test(node))
	        return node;
	    for(Node child : node.getChildren()) {
	        Node result = find(child, predicate);
	        if(result != null)
	            return result;
	    }
	    return null;
	}
}
