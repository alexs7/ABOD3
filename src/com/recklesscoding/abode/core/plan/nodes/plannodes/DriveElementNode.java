package com.recklesscoding.abode.core.plan.nodes.plannodes;

import com.recklesscoding.abode.core.plan.nodes.PlanElementNode;
import com.recklesscoding.abode.core.plan.planelements.PlanElement;
import javafx.scene.paint.Color;

/**
 * <p>
 *
 * @author :   Andreas Theodorou - www.recklesscoding.com
 * @version :   %G%
 * @see PlanElementNode
 * </p>
 */
public class DriveElementNode extends PlanElementNode {

    public DriveElementNode(PlanElement planElement) {
        super(planElement, Color.ORANGERED);
    }
}