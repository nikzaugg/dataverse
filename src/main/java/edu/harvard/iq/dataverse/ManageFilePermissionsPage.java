/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class ManageFilePermissionsPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(ManageFilePermissionsPage.class.getCanonicalName());

    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataFileServiceBean datafileService;
    @EJB
    DataverseRoleServiceBean roleService;
    @EJB
    RoleAssigneeServiceBean roleAssigneeService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    AuthenticationServiceBean authenticationService;
    @EJB 
    GroupServiceBean groupService;    
    @EJB
    EjbDataverseEngine commandEngine;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    EntityManager em;

    @Inject
    DataverseSession session;

    Dataset dataset = new Dataset(); 
    private Map<RoleAssignee,List<RoleAssignmentRow>> roleAssigneeMap = new HashMap();
    private Map<DataFile,List<RoleAssignmentRow>> fileMap = new HashMap();
    private Map<AuthenticatedUser,List<DataFile>> fileAccessRequestMap = new HashMap();    

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }
    
    public Map<RoleAssignee, List<RoleAssignmentRow>> getRoleAssigneeMap() {
        return roleAssigneeMap;
    }

    public Map<DataFile, List<RoleAssignmentRow>> getFileMap() {
        return fileMap;
    }

    public Map<AuthenticatedUser, List<DataFile>> getFileAccessRequestMap() {
        return fileAccessRequestMap;
    }
   

    public String init() {
        if (dataset.getId() != null) {
            dataset = datasetService.find(dataset.getId());
        }

        // check if dvObject exists and user has permission
        if (dataset == null) {
            return "/404.xhtml";
        }

        if (!permissionService.on(dataset).has(Permission.ManageDatasetPermissions)) {
            return "/loginpage.xhtml" + DataverseHeaderFragment.getRedirectPage();
        }
        
        initMaps();
        
        return "";
    }
    
    private void initMaps() {
        // initialize files and usergroup list
        roleAssigneeMap.clear();
        fileMap.clear();
        fileAccessRequestMap.clear();        
               
        for (DataFile file : dataset.getFiles()) {
            // only include if the file is restricted (or it's draft version is restricted)
            if (file.isRestricted() || file.getFileMetadata().isRestricted()) {
                // we get the direct role assignments assigned to the file
                List<RoleAssignment> ras = roleService.directRoleAssignments(file);
                List raList = new ArrayList<>(ras.size());
                for (RoleAssignment ra : ras) {
                    // for files, only show role assignments which can download
                    if (ra.getRole().permissions().contains(Permission.DownloadFile)) {
                        raList.add(new RoleAssignmentRow(ra, roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier()).getDisplayInfo()));                   
                        addFileToRoleAssignee(ra);                    
                    }
                }
                
                fileMap.put(file, raList);
                
                // populate the file access requests map
                for (AuthenticatedUser au : file.getFileAccessRequesters()) {
                        List<DataFile> requestedFiles = fileAccessRequestMap.get(au);
                        if (requestedFiles == null) {
                            requestedFiles = new ArrayList();
                            fileAccessRequestMap.put(au, requestedFiles);
                        }

                        requestedFiles.add(file);                    
                    
                }
            }  
        }
        
    }
    
    private void addFileToRoleAssignee(RoleAssignment assignment) {
        RoleAssignee ra = roleAssigneeService.getRoleAssignee(assignment.getAssigneeIdentifier());
        List<RoleAssignmentRow> assignments = roleAssigneeMap.get(ra);
        if (assignments == null) {
            assignments = new ArrayList();
            roleAssigneeMap.put(ra, assignments);
        }
        
        assignments.add(new RoleAssignmentRow(assignment, ra.getDisplayInfo()));
    }

    /* 
     main page
     */
    
    public void removeRoleAssignments(List<RoleAssignmentRow> raRows) {
        for (RoleAssignmentRow raRow : raRows) {
            revokeRole(raRow.getId());
        }
        
        initMaps();
        showUserGroupMessages();
    }    


    /*
     view / remove roles dialog
     */  
    private DataFile selectedFile;
    private RoleAssignee selectedRoleAssignee;
    private List<RoleAssignmentRow> roleAssignments;
    private List<RoleAssignmentRow> selectedRoleAssignmentRows;

    public DataFile getSelectedFile() {
        return selectedFile;
    }

    public void setSelectedFile(DataFile selectedFile) {
        this.selectedFile = selectedFile;
    }

    public RoleAssignee getSelectedRoleAssignee() {
        return selectedRoleAssignee;
    }

    public void setSelectedRoleAssignee(RoleAssignee selectedRoleAssignee) {
        this.selectedRoleAssignee = selectedRoleAssignee;
    }
    
    public List<RoleAssignmentRow> getRoleAssignments() {
        return roleAssignments;
    }

    public void setRoleAssignments(List<RoleAssignmentRow> roleAssignments) {
        this.roleAssignments = roleAssignments;
    }

    public List<RoleAssignmentRow> getSelectedRoleAssignmentRows() {
        return selectedRoleAssignmentRows;
    }

    public void setSelectedRoleAssignmentRows(List<RoleAssignmentRow> selectedRoleAssignmentRows) {
        this.selectedRoleAssignmentRows = selectedRoleAssignmentRows;
    }
    
    public void initViewRemoveDialogByFile(DataFile file, List<RoleAssignmentRow> raRows) {
        this.selectedFile = file;
        this.selectedRoleAssignee = null;
        this.roleAssignments = raRows;
        showFileMessages();
    }
    
    public void initViewRemoveDialogByRoleAssignee(RoleAssignee ra, List<RoleAssignmentRow> raRows) {
        this.selectedFile = null;
        this.selectedRoleAssignee = ra;
        this.roleAssignments = raRows;
        showUserGroupMessages();
    }    
    
    public void removeRoleAssignments() {
        for (RoleAssignmentRow raRow : selectedRoleAssignmentRows) {
            revokeRole(raRow.getId());
        }
        
        initMaps();        
    }
    
    // internal method used by removeRoleAssignments
    private void revokeRole(Long roleAssignmentId) {
        try {
            RoleAssignment ra = em.find(RoleAssignment.class, roleAssignmentId);
            commandEngine.submit(new RevokeRoleCommand(ra, session.getUser()));
            JsfHelper.addSuccessMessage(ra.getRole().getName() + " role for " + roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier()).getDisplayInfo().getTitle() + " was removed.");
        } catch (PermissionException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, "The role assignment was not able to be removed.", "Permissions " + ex.getRequiredPermissions().toString() + " missing.");
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, "The role assignment could not be removed.");
            logger.log(Level.SEVERE, "Error removing role assignment: " + ex.getMessage(), ex);
        }
    }    
  
 
    /*
     grant access dialog
     */
    private List<RoleAssignee> selectedRoleAssignees;
    private List<DataFile> selectedFiles = new ArrayList();
    private List<RoleAssignee> roleAssigneeList = new ArrayList();

    public List<RoleAssignee> getSelectedRoleAssignees() {
        return selectedRoleAssignees;
    }

    public void setSelectedRoleAssignees(List<RoleAssignee> selectedRoleAssignees) {
        this.selectedRoleAssignees = selectedRoleAssignees;
    }

    public List<DataFile> getSelectedFiles() {
        return selectedFiles;
    }

    public void setSelectedFiles(List<DataFile> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }


    public void initAssignDialog(ActionEvent ae) {
        selectedRoleAssignees = null;
        selectedFiles.clear();
        showUserGroupMessages();
    }
    
    public void initAssignDialogByFile(DataFile file) {
        selectedRoleAssignees = null;
        selectedFiles.clear();
        selectedFiles.add(file);
        showFileMessages();
    }
    public void initAssignDialogByUser(AuthenticatedUser au) {
        selectedRoleAssignees = new ArrayList();
        selectedFiles.clear();
        selectedRoleAssignees.add(au);
        selectedFiles.addAll(fileAccessRequestMap.get(au));
        
        showUserGroupMessages();
    }     
    

    public List<RoleAssignee> completeRoleAssignee(String query) {
        if (roleAssigneeList.isEmpty()) {
            for (AuthenticatedUser au : authenticationService.findAllAuthenticatedUsers()) {
                roleAssigneeList.add(au);
            }
            for ( Group g : groupService.findAllGroups() ) {
                roleAssigneeList.add( g );
            }            
        }
        List<RoleAssignee> returnList = new ArrayList();
        for (RoleAssignee ra : roleAssigneeList) {
            // @todo unsure if containsIgnore case will work for all locales
            if (StringUtils.containsIgnoreCase(ra.getDisplayInfo().getTitle(), query) && (selectedRoleAssignees == null || !selectedRoleAssignees.contains(ra))) {
                returnList.add(ra);
            }
        }
        return returnList;
    }
    
    public void grantAccess(ActionEvent evt) {
        // Find the built in file downloader role (currently by alias)
        DataverseRole fileDownloaderRole = roleService.findBuiltinRoleByAlias(DataverseRole.FILE_DOWNLOADER);
        for (RoleAssignee roleAssignee : selectedRoleAssignees) {
            for (DataFile file : selectedFiles) {
                assignRole(roleAssignee, file, fileDownloaderRole);                
            
                // remove any requests, if they exist
                file.getFileAccessRequesters().remove(roleAssignee);
                datafileService.save(file);
            }
        }
        
        initMaps();
    }
    
    public void grantAccessToRequests(AuthenticatedUser au) {
        // Find the built in file downloader role (currently by alias) 
        DataverseRole fileDownloaderRole = roleService.findBuiltinRoleByAlias(DataverseRole.FILE_DOWNLOADER);
        for (DataFile file : fileAccessRequestMap.get(au)) {
            assignRole(au, file, fileDownloaderRole);                

            file.getFileAccessRequesters().remove(au);
            datafileService.save(file);
        }

        
        initMaps();
    }    

    private void assignRole(RoleAssignee ra,  DataFile file, DataverseRole r) {
        try {
            commandEngine.submit(new AssignRoleCommand(ra, r, file, session.getUser()));
            JsfHelper.addSuccessMessage(r.getName() + " role assigned to " + ra.getDisplayInfo().getTitle() + " for " + file.getDisplayName() + ".");
        } catch (PermissionException ex) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, "The role was not able to be assigned.", "Permissions " + ex.getRequiredPermissions().toString() + " missing.");
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, "The role was not able to be assigned.");
            logger.log(Level.SEVERE, "Error assiging role: " + ex.getMessage(), ex);
        }
    }


    boolean renderUserGroupMessages = false;
    boolean renderFileMessages = false;
       
    private void showUserGroupMessages() {
        renderUserGroupMessages = true;
        renderFileMessages = false;
    }

    private void showFileMessages() {
        renderUserGroupMessages = false;
        renderFileMessages = true;
    }    
    
    public boolean isRenderUserGroupMessages() {
        return renderUserGroupMessages;
    }

    public void setRenderUserGroupMessages(boolean renderUserGroupMessages) {
        this.renderUserGroupMessages = renderUserGroupMessages;
    }

    public boolean isRenderFileMessages() {
        return renderFileMessages;
    }

    public void setRenderFileMessages(boolean renderFileMessages) {
        this.renderFileMessages = renderFileMessages;
    }





    // inner class used fordisplay of role assignments
    public static class RoleAssignmentRow {

        private final RoleAssigneeDisplayInfo assigneeDisplayInfo;
        private final RoleAssignment ra;

        public RoleAssignmentRow(RoleAssignment anRa, RoleAssigneeDisplayInfo disInf) {
            this.ra = anRa;
            this.assigneeDisplayInfo = disInf;
        }        
        

        public RoleAssigneeDisplayInfo getAssigneeDisplayInfo() {
            return assigneeDisplayInfo;
        }

        public DvObject getDefinitionPoint() {
            return ra.getDefinitionPoint();
        }

        
        public Long getId() {
            return ra.getId();
        }
    
    }   
}
