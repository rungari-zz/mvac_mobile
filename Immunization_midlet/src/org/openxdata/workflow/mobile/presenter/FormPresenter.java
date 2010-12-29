package org.openxdata.workflow.mobile.presenter;

import java.util.Date;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;

import org.openxdata.db.OpenXdataDataStorage;
import org.openxdata.db.util.Persistent;
import org.openxdata.db.util.StorageListener;
import org.openxdata.forms.FormManager;
import org.openxdata.model.FormData;
import org.openxdata.model.FormDef;
import org.openxdata.model.QuestionData;
import org.openxdata.model.UserListStudyDefList;
import org.openxdata.workflow.mobile.DownloadManager;
import org.openxdata.workflow.mobile.FormListenerAdaptor;
import org.openxdata.workflow.mobile.command.ActionDispatcher;
import org.openxdata.workflow.mobile.command.ActionListener;
import org.openxdata.workflow.mobile.command.WFCommands;
import org.openxdata.workflow.mobile.db.WFStorage;
import org.openxdata.workflow.mobile.model.MQuestionMap;
import org.openxdata.workflow.mobile.model.MWorkItem;
import org.openxdata.workflow.mobile.util.AlertMsgHelper;

public class FormPresenter extends FormListenerAdaptor implements
        ActionListener, StorageListener {

        private final Display display;
        private FormManager formManager;
        private DownloadManager dldMgr;
        private MWorkItem workItem;
        private Displayable disp;

        public FormPresenter(Display display, FormManager formManager,
                DownloadManager manager, ActionDispatcher dispatcher) {
                this.display = display;
                this.formManager = formManager;
                this.dldMgr = manager;
                dispatcher.registerListener(WFCommands.cmdOpenWir, this);

        }

        public boolean handle(Command cmd, Displayable disp, Object obj) {
                if (cmd != WFCommands.cmdOpenWir) {
                        return false;
                }
                this.workItem = (MWorkItem) obj;
                this.disp = disp;
                showFormFor((MWorkItem) obj, disp);
                return true;
        }

        public boolean beforeFormDisplay(FormData frmData) {
                // Do form prefilling before the form is displayed
                String formName = frmData.getDef().getVariableName();
                Vector prefilledQns = workItem.getPrefilledQns();
                for (int i = 0; i < prefilledQns.size(); i++) {
                        MQuestionMap qnMap = (MQuestionMap) prefilledQns.elementAt(i);
                        String questionName = "/" + formName + "/" + qnMap.getQuestion();
                        QuestionData qnData = frmData.getQuestion(questionName);
                        qnData.setTextAnswer(qnMap.getValue());
                        if (qnMap.isOutput()) {
                                qnData.getDef().setEnabled(true);
                        } else {
                                qnData.getDef().setEnabled(false);
                        }
                }
                return true;
        }

        public void showFormFor(MWorkItem wir, Displayable origScreen) {
                this.workItem = wir;
                FormDef formDef = WFStorage.getFormDefForWorkItem(wir);
                if (formDef == null) {
                        dldMgr.downloadStudies(this);
                } else {
                        showForm(formDef, origScreen);
                }
        }

        private void showForm(FormDef formDef, Displayable origDisplay) {
                FormData formData = WFStorage.getOrCreateFormData(workItem, formDef);
                workItem.setDataRecId(new Integer(formData.getRecordId()));
                WFStorage.saveMWorkItem(workItem, this);
                formManager.showForm(true, formData, true, origDisplay);
        }

        public void downloaded(Persistent dataOutParams, Persistent dataOut) {
                // TODO check for empty studies
                if (dataOut instanceof UserListStudyDefList) {
                        handleStudyAndUserDownloaded((UserListStudyDefList) dataOut);
                }
        }

        private void handleStudyAndUserDownloaded(
                UserListStudyDefList userStudyDefList) {
                WFStorage.saveUserListStudyDefList(userStudyDefList);
                showFormFor(workItem, disp);
        }

        public void errorOccured(String errorMessage, Exception e) {
                AlertMsgHelper.showMsg(display, disp, errorMessage);
                e.printStackTrace();
        }

        public void saveFormData(FormData data, boolean isNew) {
                String formName = data.getDef().getVariableName();
                data.setDateValue("/" + formName + "/endtime", new Date());
                OpenXdataDataStorage.saveFormData(workItem.getStudyId(), data);
        }

        public boolean beforeFormSaved(FormData data, boolean isNew) {
                saveFormData(data, isNew);
                display.setCurrent(disp);
                return false;
        }

        public boolean beforeFormDelete(FormData data) {
                workItem.setDataRecId(new Integer(-1));
                WFStorage.saveMWorkItem(workItem, this);
                OpenXdataDataStorage.deleteFormData(workItem.getStudyId(), data);
                display.setCurrent(disp);
                return false;
        }
}
