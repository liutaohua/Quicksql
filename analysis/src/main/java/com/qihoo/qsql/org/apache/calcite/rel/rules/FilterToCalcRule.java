/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qihoo.qsql.org.apache.calcite.rel.rules;

import com.qihoo.qsql.org.apache.calcite.plan.RelOptRule;
import com.qihoo.qsql.org.apache.calcite.plan.RelOptRuleCall;
import com.qihoo.qsql.org.apache.calcite.rel.RelNode;
import com.qihoo.qsql.org.apache.calcite.rel.core.RelFactories;
import com.qihoo.qsql.org.apache.calcite.rel.logical.LogicalCalc;
import com.qihoo.qsql.org.apache.calcite.rel.logical.LogicalFilter;
import com.qihoo.qsql.org.apache.calcite.rel.type.RelDataType;
import com.qihoo.qsql.org.apache.calcite.rex.RexBuilder;
import com.qihoo.qsql.org.apache.calcite.rex.RexProgram;
import com.qihoo.qsql.org.apache.calcite.rex.RexProgramBuilder;
import com.qihoo.qsql.org.apache.calcite.tools.RelBuilderFactory;

/**
 * Planner rule that converts a
 * {@link com.qihoo.qsql.org.apache.calcite.rel.logical.LogicalFilter} to a
 * {@link com.qihoo.qsql.org.apache.calcite.rel.logical.LogicalCalc}.
 *
 * <p>The rule does <em>NOT</em> fire if the child is a
 * {@link com.qihoo.qsql.org.apache.calcite.rel.logical.LogicalFilter} or a
 * {@link com.qihoo.qsql.org.apache.calcite.rel.logical.LogicalProject} (we assume they they
 * will be converted using {@link FilterToCalcRule} or
 * {@link ProjectToCalcRule}) or a
 * {@link com.qihoo.qsql.org.apache.calcite.rel.logical.LogicalCalc}. This
 * {@link com.qihoo.qsql.org.apache.calcite.rel.logical.LogicalFilter} will eventually be
 * converted by {@link FilterCalcMergeRule}.
 */
public class FilterToCalcRule extends RelOptRule {
  //~ Static fields/initializers ---------------------------------------------

  public static final FilterToCalcRule INSTANCE =
      new FilterToCalcRule(RelFactories.LOGICAL_BUILDER);

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a FilterToCalcRule.
   *
   * @param relBuilderFactory Builder for relational expressions
   */
  public FilterToCalcRule(RelBuilderFactory relBuilderFactory) {
    super(operand(LogicalFilter.class, any()), relBuilderFactory, null);
  }

  //~ Methods ----------------------------------------------------------------

  public void onMatch(RelOptRuleCall call) {
    final LogicalFilter filter = call.rel(0);
    final RelNode rel = filter.getInput();

    // Create a program containing a filter.
    final RexBuilder rexBuilder = filter.getCluster().getRexBuilder();
    final RelDataType inputRowType = rel.getRowType();
    final RexProgramBuilder programBuilder =
        new RexProgramBuilder(inputRowType, rexBuilder);
    programBuilder.addIdentity();
    programBuilder.addCondition(filter.getCondition());
    final RexProgram program = programBuilder.getProgram();

    final LogicalCalc calc = LogicalCalc.create(rel, program);
    call.transformTo(calc);
  }
}

// End FilterToCalcRule.java