package org.activiti.designer.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.activiti.bpmn.model.BoundaryEvent;
import org.activiti.bpmn.model.Message;
import org.activiti.bpmn.model.MessageEventDefinition;
import org.activiti.bpmn.model.Signal;
import org.activiti.designer.util.eclipse.ActivitiUiUtil;
import org.activiti.designer.util.editor.Bpmn2MemoryModel;
import org.activiti.designer.util.editor.ModelHandler;
import org.activiti.designer.util.property.ActivitiPropertySection;
import org.apache.commons.lang.StringUtils;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

public class PropertyBoundaryMessageSection extends ActivitiPropertySection implements ITabbedPropertyConstants {

  private CCombo cancelActivityCombo;
  private List<String> cancelFormats = Arrays.asList("true", "false");
  private CCombo messageCombo;

  @Override
  public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage) {
    super.createControls(parent, tabbedPropertySheetPage);

    TabbedPropertySheetWidgetFactory factory = getWidgetFactory();
    Composite composite = factory.createFlatFormComposite(parent);
    FormData data;

    cancelActivityCombo = factory.createCCombo(composite, SWT.NONE);
    cancelActivityCombo.setItems((String[]) cancelFormats.toArray());
    data = new FormData();
    data.left = new FormAttachment(0, 160);
    data.right = new FormAttachment(100, 0);
    data.top = new FormAttachment(0, VSPACE);
    cancelActivityCombo.setLayoutData(data);
    cancelActivityCombo.addFocusListener(listener);

    createLabel(composite, "Cancel activity", cancelActivityCombo, factory); //$NON-NLS-1$

    messageCombo = getWidgetFactory().createCCombo(composite, SWT.NONE);
    data = new FormData();
    data.left = new FormAttachment(0, 160);
    data.right = new FormAttachment(100, -HSPACE);
    data.top = new FormAttachment(cancelActivityCombo, VSPACE);
    messageCombo.setLayoutData(data);
    messageCombo.addFocusListener(listener);

    createLabel(composite, "Message ref", messageCombo, factory); //$NON-NLS-1$
  }

  @Override
  public void refresh() {
    cancelActivityCombo.removeFocusListener(listener);
    messageCombo.removeFocusListener(listener);

    PictogramElement pe = getSelectedPictogramElement();
    if (pe != null) {
      Object bo = getBusinessObject(pe);
      if (bo == null)
        return;

      final Bpmn2MemoryModel model = ModelHandler.getModel(EcoreUtil.getURI(getDiagram()));
      if (model == null) {
        return;
      }

      boolean cancelActivity = ((BoundaryEvent) bo).isCancelActivity();
      if (cancelActivity == false) {
        cancelActivityCombo.select(1);
      } else {
        cancelActivityCombo.select(0);
      }

      String messageRef = null;
      if (bo instanceof BoundaryEvent) {
        BoundaryEvent boundaryEvent = (BoundaryEvent) bo;
        if (boundaryEvent.getEventDefinitions().get(0) != null) {
          MessageEventDefinition messageDefinition = (MessageEventDefinition) boundaryEvent.getEventDefinitions().get(0);
          if (StringUtils.isNotEmpty(messageDefinition.getMessageRef())) {
            messageRef = messageDefinition.getMessageRef();
          }
        }
      }

      String[] items = new String[model.getBpmnModel().getMessages().size() + 1];
      items[0] = "";
      int counter = 1;
      int selectedCounter = 0;
      for (Message message : model.getBpmnModel().getMessages()) {
        items[counter] = message.getId() + " / " + message.getName();
        if (message.getId().equals(messageRef)) {
          selectedCounter = counter;
        }
        counter++;
      }

      messageCombo.setItems(items);
      messageCombo.select(selectedCounter);
    }
    cancelActivityCombo.addFocusListener(listener);
    messageCombo.addFocusListener(listener);
  }

  private FocusListener listener = new FocusListener() {

    public void focusGained(final FocusEvent e) {
    }

    public void focusLost(final FocusEvent e) {
      final Bpmn2MemoryModel model = ModelHandler.getModel(EcoreUtil.getURI(getDiagram()));
      if (model == null) {
        return;
      }

      PictogramElement pe = getSelectedPictogramElement();
      if (pe != null) {
        final Object bo = getBusinessObject(pe);
        if (bo instanceof BoundaryEvent) {
          DiagramEditor diagramEditor = (DiagramEditor) getDiagramEditor();
          TransactionalEditingDomain editingDomain = diagramEditor.getEditingDomain();
          ActivitiUiUtil.runModelChange(new Runnable() {

            public void run() {

              if (bo instanceof BoundaryEvent) {
                BoundaryEvent boundaryEvent = (BoundaryEvent) bo;

                int selection = cancelActivityCombo.getSelectionIndex();
                if (selection == 0) {
                  boundaryEvent.setCancelActivity(true);
                } else {
                  boundaryEvent.setCancelActivity(false);
                }

                MessageEventDefinition messageDefinition = (MessageEventDefinition) boundaryEvent.getEventDefinitions().get(0);
                if (messageCombo.getSelectionIndex() > 0) {
                  List<Message> messageList = new ArrayList<Message>(model.getBpmnModel().getMessages());
                  messageDefinition.setMessageRef(messageList.get(messageCombo.getSelectionIndex() - 1).getId());
                } else {
                  messageDefinition.setMessageRef("");
                }
              }
            }
          }, editingDomain, "Model Update");
        }

      }
    }
  };

  private CLabel createLabel(Composite parent, String text, Control control, TabbedPropertySheetWidgetFactory factory) {
    CLabel label = factory.createCLabel(parent, text); //$NON-NLS-1$
    FormData data = new FormData();
    data.left = new FormAttachment(0, 0);
    data.right = new FormAttachment(control, -HSPACE);
    data.top = new FormAttachment(control, 0, SWT.TOP);
    label.setLayoutData(data);
    return label;
  }
}
