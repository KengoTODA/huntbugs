/*
 * Copyright 2016 HuntBugs contributors
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
package one.util.huntbugs.detect;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.strobel.assembler.metadata.MethodReference;
import com.strobel.decompiler.ast.AstCode;
import com.strobel.decompiler.ast.Expression;

import one.util.huntbugs.registry.MethodContext;
import one.util.huntbugs.registry.anno.AstNodes;
import one.util.huntbugs.registry.anno.AstVisitor;
import one.util.huntbugs.registry.anno.MethodVisitor;
import one.util.huntbugs.registry.anno.WarningDefinition;
import one.util.huntbugs.util.Equi;
import one.util.huntbugs.util.Methods;
import one.util.huntbugs.util.Nodes;
import one.util.huntbugs.warning.Role.StringRole;

/**
 * @author Tagir Valeev
 *
 */
@WarningDefinition(category = "Correctness", name = "AndEqualsAlwaysFalse", maxScore = 85)
@WarningDefinition(category = "Correctness", name = "OrNotEqualsAlwaysTrue", maxScore = 70)
public class ExclusiveConditions {
    private static final StringRole CONST1 = StringRole.forName("CONST1");
    private static final StringRole CONST2 = StringRole.forName("CONST2");

    Set<Expression> reported;

    @MethodVisitor
    public void init() {
        reported = new HashSet<>();
    }

    @AstVisitor(nodes = AstNodes.EXPRESSIONS)
    public void visit(Expression expr, MethodContext mc) {
        if (expr.getCode() == AstCode.LogicalAnd) {
            if (Nodes.isSideEffectFree(expr)) {
                Expression left = expr.getArguments().get(0);
                Expression right = expr.getArguments().get(1);
                if (isEquality(left)) {
                    checkEqual(left, right, mc);
                } else if (isEquality(right)) {
                    checkEqual(right, left, mc);
                }
            }
        }
        if (expr.getCode() == AstCode.LogicalOr) {
            if (Nodes.isSideEffectFree(expr)) {
                Expression left = expr.getArguments().get(0);
                Expression right = expr.getArguments().get(1);
                if (isNonEquality(left)) {
                    checkNonEqual(left, right, mc);
                } else if (isNonEquality(right)) {
                    checkNonEqual(right, left, mc);
                }
            }
        }
    }

    private boolean isEquality(Expression expr) {
        if (expr.getCode() == AstCode.CmpEq)
            return true;
        if (expr.getCode() == AstCode.InvokeVirtual) {
            return Methods.isEqualsMethod((MethodReference) expr.getOperand());
        }
        return false;
    }

    private boolean isNonEquality(Expression expr) {
        if (expr.getCode() == AstCode.CmpNe)
            return true;
        if (expr.getCode() == AstCode.Neg) {
            Expression arg = expr.getArguments().get(0);
            if (arg.getCode() == AstCode.InvokeVirtual) {
                return Methods.isEqualsMethod((MethodReference) arg.getOperand());
            }
        }
        return false;
    }

    private void checkEqual(Expression equality, Expression other, MethodContext mc) {
        Nodes.ifBinaryWithConst(equality, (arg, constant) -> {
            if (isEquality(other)) {
                Expression left = other.getArguments().get(0);
                Expression right = other.getArguments().get(1);
                Expression arg2 = null;
                Object constant2 = null;
                if(Equi.equiExpressions(arg, left)) {
                    constant2 = Nodes.getConstant(right);
                    arg2 = left;
                } else if(Equi.equiExpressions(arg, right)) {
                    constant2 = Nodes.getConstant(left);
                    arg2 = right;
                }
                if(constant2 != null && !Objects.equals(constant, constant2)) {
                    // non-short-circuit logic is intended
                    if (reported.add(arg) & reported.add(arg2)) {
                        mc.report("AndEqualsAlwaysFalse", 0, arg, CONST1.createFromConst(constant), CONST2
                                .createFromConst(constant2));
                    }
                }
            }
            if (other.getCode() == AstCode.LogicalAnd) {
                checkEqual(equality, other.getArguments().get(0), mc);
                checkEqual(equality, other.getArguments().get(1), mc);
            }
        });
    }

    private void checkNonEqual(Expression equality, Expression other, MethodContext mc) {
        Nodes.ifBinaryWithConst(equality, (arg, constant) -> {
            if (isNonEquality(other)) {
                Expression left = other.getArguments().get(0);
                Expression right = other.getArguments().get(1);
                Expression arg2 = null;
                Object constant2 = null;
                if(Equi.equiExpressions(arg, left)) {
                    constant2 = Nodes.getConstant(right);
                    arg2 = left;
                } else if(Equi.equiExpressions(arg, right)) {
                    constant2 = Nodes.getConstant(left);
                    arg2 = right;
                }
                if(constant2 != null && !Objects.equals(constant, constant2)) {
                    // non-short-circuit logic is intended
                    if (reported.add(arg) & reported.add(arg2)) {
                        mc.report("OrNotEqualsAlwaysTrue", 0, arg, CONST1.createFromConst(constant), CONST2
                                .createFromConst(constant2));
                    }
                }
            }
            if (other.getCode() == AstCode.LogicalOr) {
                checkNonEqual(equality, other.getArguments().get(0), mc);
                checkNonEqual(equality, other.getArguments().get(1), mc);
            }
        });
    }
}
