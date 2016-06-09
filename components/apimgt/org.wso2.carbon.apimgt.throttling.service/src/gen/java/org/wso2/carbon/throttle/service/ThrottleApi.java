package org.wso2.carbon.throttle.service;

import org.wso2.carbon.throttle.service.dto.*;
import org.wso2.carbon.throttle.service.ThrottleApiService;
import org.wso2.carbon.throttle.service.factories.ThrottleApiServiceFactory;

import io.swagger.annotations.ApiParam;

import org.wso2.carbon.throttle.service.dto.ErrorDTO;
import org.wso2.carbon.throttle.service.dto.ThrottledEventDTO;

import java.util.List;

import java.io.InputStream;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

import javax.ws.rs.core.Response;
import javax.ws.rs.*;

@Path("/throttle")

@Produces({ "application/json" })
@io.swagger.annotations.Api(value = "/throttle", description = "the throttle API")
public class ThrottleApi  {

   private final ThrottleApiService delegate = ThrottleApiServiceFactory.getThrottleApi();

    @GET
    
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "throttled events available", notes = "This will provide access to throttled events in database.", response = ThrottledEventDTO.class, responseContainer = "List")
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "An array of shops around you"),
        
        @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error") })

    public Response throttleGet(@ApiParam(value = "**Search condition**.\n\nYou can search for an application by specifying the name as \"query\" attribute.\n\nEg.\n\"app1\" will match an application if the name is exactly \"app1\".\n\nCurrently this does not support wildcards. Given name must exactly match the application name.") @QueryParam("query") String query)
    {
    return delegate.throttleGet(query);
    }
}

