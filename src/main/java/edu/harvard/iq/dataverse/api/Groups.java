package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.shib.ShibGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.shib.ShibGroupProvider;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
/**
 *
 * @author michael
 */
@Path("admin/groups")
@Stateless
public class Groups extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(Groups.class.getName());
    
    private IpGroupProvider ipGroupPrv;
    private ShibGroupProvider shibGroupPrv;
    
    @PostConstruct
    void postConstruct() {
        ipGroupPrv = groupSvc.getIpGroupProvider();
        shibGroupPrv = groupSvc.getShibGroupProvider();
    }
    
    @POST
    @Path("ip")
    public Response createIpGroups( JsonObject dto ){
        try {
           IpGroup grp = new JsonParser().parseIpGroup(dto);
            
            if ( grp.getPersistedGroupAlias()== null ) {
                return error(Response.Status.BAD_REQUEST, "Must provide valid group alias");
            }
            grp.setProvider( groupSvc.getIpGroupProvider() );
            
            grp = ipGroupPrv.store(grp);
            return created("/groups/ip/" + grp.getPersistedGroupAlias(), json(grp) );
        
        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Error while storing a new IP group: " + e.getMessage(), e);
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage() );
            
        }
    }
    
    
    @GET
    @Path("ip")
    public Response listIpGroups() {
        return ok( ipGroupPrv.findGlobalGroups()
                             .stream().map(g->json(g)).collect(toJsonArray()) );
    }
    
    @GET
    @Path("ip/{groupIdtf}")
    public Response getIpGroup( @PathParam("groupIdtf") String groupIdtf ) {
        IpGroup grp;
        if ( isNumeric(groupIdtf) ) {
            grp = ipGroupPrv.get( Long.parseLong(groupIdtf) );
        } else {
            grp = ipGroupPrv.get(groupIdtf);
        }
        
        return (grp == null) ? notFound( "Group " + groupIdtf + " not found") : ok(json(grp));
    }
    
    @DELETE
    @Path("ip/{groupIdtf}")
    public Response deleteIpGroup( @PathParam("groupIdtf") String groupIdtf ) {
        IpGroup grp;
        if ( isNumeric(groupIdtf) ) {
            grp = ipGroupPrv.get( Long.parseLong(groupIdtf) );
        } else {
            grp = ipGroupPrv.get(groupIdtf);
        }
        
        if (grp == null) return notFound( "Group " + groupIdtf + " not found");
        
        try {
            ipGroupPrv.deleteGroup(grp);
            return ok("Group " + grp.getAlias() + " deleted.");
        } catch ( Exception topExp ) {
            // get to the cause (unwraps EJB exception wrappers).
            Throwable e = topExp;
            while ( e.getCause() != null ) {
                e = e.getCause();
            }
            
            if ( e instanceof IllegalArgumentException ) {
                return error(Response.Status.BAD_REQUEST, e.getMessage());
            } else {
                throw topExp;
            }
        }
    }
    
    @GET
    @Path("shib")
    public Response listShibGroups() {
        JsonArrayBuilder arrBld = Json.createArrayBuilder();
        for (ShibGroup g : shibGroupPrv.findGlobalGroups()) {
            arrBld.add(json(g));
        }
        return ok(arrBld);
    }

    @POST
    @Path("shib")
    public Response createShibGroup(JsonObject shibGroupInput) {
        String expectedNameKey = "name";
        JsonString name = shibGroupInput.getJsonString(expectedNameKey);
        if (name == null) {
            return error(Response.Status.BAD_REQUEST, "required field missing: " + expectedNameKey);
        }
        String expectedAttributeKey = "attribute";
        JsonString attribute = shibGroupInput.getJsonString(expectedAttributeKey);
        if (attribute == null) {
            return error(Response.Status.BAD_REQUEST, "required field missing: " + expectedAttributeKey);
        }
        String expectedPatternKey = "pattern";
        JsonString pattern = shibGroupInput.getJsonString(expectedPatternKey);
        if (pattern == null) {
            return error(Response.Status.BAD_REQUEST, "required field missing: " + expectedPatternKey);
        }
        ShibGroup shibGroupToPersist = new ShibGroup(name.getString(), attribute.getString(), pattern.getString(), shibGroupPrv);
        ShibGroup persitedShibGroup = shibGroupPrv.persist(shibGroupToPersist);
        if (persitedShibGroup != null) {
            return ok("Shibboleth group persisted: " + persitedShibGroup);
        } else {
            return error(Response.Status.BAD_REQUEST, "Could not persist Shibboleth group");
        }
    }

    @DELETE
    @Path("shib/{primaryKey}")
    public Response deleteShibGroup( @PathParam("primaryKey") String id ) {
        ShibGroup doomed = shibGroupPrv.get(id);
        if (doomed != null) {
            boolean deleted;
            try {
                deleted = shibGroupPrv.delete(doomed);
            } catch (Exception ex) {
                return error(Response.Status.BAD_REQUEST, ex.getMessage());
            }
            if (deleted) {
                return ok("Shibboleth group " + id + " deleted");
            } else {
                return error(Response.Status.BAD_REQUEST, "Could not delete Shibboleth group with an id of " + id);
            }
        } else {
            return error(Response.Status.BAD_REQUEST, "Could not find Shibboleth group with an id of " + id);
        }
    }
    
}
