package com.recklesscoding.abode.gui.nodemenu.controller.editelement;

import com.recklesscoding.abode.core.plan.nodes.PlanElementNode;
import com.recklesscoding.abode.gui.nodemenu.controller.newelement.NewPlanElementMenuItem;
import com.recklesscoding.abode.gui.nodemenu.popups.editelement.EditCompetenceElement;
import com.recklesscoding.abode.gui.nodemenu.popups.newelement.NewCompetenceElement;
import com.recklesscoding.abode.gui.views.diagramview.diagram.GraphWindow;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * <p>
 *
 * @author :   Andreas Theodorou - www.recklesscoding.com
 * @version :   %G%
 */
public class EditCompetenceElemMenuItem extends EditPlanElementMenuItem {

    public EditCompetenceElemMenuItem(Stage stage, PlanElementNode planElementNode, GraphWindow graphWindow) {
        super(stage, planElementNode, graphWindow);
    }

    @Override
    void initButtonAction(Stage stage, PlanElementNode planElementNode, GraphWindow graphWindow) {
        setOnAction(t -> Platform.runLater(() -> new EditCompetenceElement(planElementNode, graphWindow)));
    }
}
