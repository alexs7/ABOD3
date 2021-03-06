package com.recklesscoding.abode.gui.nodemenu.controller.editelement;

import com.recklesscoding.abode.core.plan.nodes.PlanElementNode;
import com.recklesscoding.abode.gui.nodemenu.popups.editelement.EditAction;
import com.recklesscoding.abode.gui.nodemenu.popups.editelement.EditActionPattern;
import com.recklesscoding.abode.gui.views.diagramview.diagram.GraphWindow;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * <p>
 *
 * @author :   Andreas Theodorou - www.recklesscoding.com
 * @version :   %G%
 */
public class EditAPMenuItem extends EditPlanElementMenuItem {

    public EditAPMenuItem(Stage stage, PlanElementNode planElementNode, GraphWindow graphWindow) {
        super(stage, planElementNode, graphWindow);
    }

    @Override
    void initButtonAction(Stage primaryStage, PlanElementNode planElementNode, GraphWindow graphWindow) {
        setOnAction(t -> {
            Platform.runLater(() -> {
                new EditActionPattern(planElementNode, graphWindow);
            });
        });
    }
}