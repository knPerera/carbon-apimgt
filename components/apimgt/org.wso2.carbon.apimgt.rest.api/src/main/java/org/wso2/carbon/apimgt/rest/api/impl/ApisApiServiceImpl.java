/*
 *  Copyright WSO2 Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.apimgt.rest.api.impl;

import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.FaultGatewaysException;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.DuplicateAPIException;
import org.wso2.carbon.apimgt.api.model.Documentation;
import org.wso2.carbon.apimgt.api.model.KeyManager;
import org.wso2.carbon.apimgt.impl.factory.KeyManagerHolder;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.rest.api.ApiResponseMessage;
import org.wso2.carbon.apimgt.rest.api.ApisApiService;
import org.wso2.carbon.apimgt.rest.api.dto.APIDTO;
import org.wso2.carbon.apimgt.rest.api.dto.DocumentDTO;
import org.wso2.carbon.apimgt.rest.api.utils.RestApiUtil;
import org.wso2.carbon.apimgt.rest.api.utils.mappings.APIMappingUtil;
import org.wso2.carbon.apimgt.rest.api.exception.InternalServerErrorException;
import org.wso2.carbon.apimgt.rest.api.exception.NotFoundException;

import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


public class ApisApiServiceImpl extends ApisApiService {

    @Override
    public Response apisGet(String limit,String offset,String query,String type,String sort,String accept,String ifNoneMatch){
        List<org.wso2.carbon.apimgt.api.model.API> apis;
        List<APIDTO> list = new ArrayList<APIDTO>();

        boolean isTenantFlowStarted = false;

        try {
            String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
            APIProvider apiProvider = RestApiUtil.getProvider(loggedInUser);
            String tenantDomain =  CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            if(tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
            }
            apis = apiProvider.searchAPIs(query,type, loggedInUser);
            for (org.wso2.carbon.apimgt.api.model.API temp : apis) {
                list.add(APIMappingUtil.fromAPItoDTO(temp));
            }
        } catch (APIManagementException e) {
            throw new InternalServerErrorException(e);
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
        return Response.ok().entity(list).build();
    }
    @Override
    public Response apisPost(APIDTO body,String contentType){

        boolean isTenantFlowStarted = false;
        URI createdApiUri = null;
        try {
            org.wso2.carbon.apimgt.api.model.API apiToAdd = APIMappingUtil.fromDTOtoAPI(body);
            String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
            APIProvider apiProvider = RestApiUtil.getProvider(loggedInUser);
            String tenantDomain =  CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            if(tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
            }
            apiProvider.addAPI(apiToAdd);
            apiProvider.saveSwagger20Definition(apiToAdd.getId(), body.getApiDefinition());
            //This was added in order to provide location header for HTTP response
            APIIdentifier createdApiId = apiToAdd.getId();
            createdApiUri = new URI("http://10.100.7.39:9763/api/am/v1/apis/"
                + createdApiId.getApiName() + "-" + createdApiId.getVersion() + "-" + createdApiId.getProviderName());
            //how to add thumbnail
            //publish to external stores
        } catch (APIManagementException e) {
            throw new InternalServerErrorException(e);
        } catch (URISyntaxException e) {
            throw new InternalServerErrorException(e);
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }

        return Response.created(createdApiUri).entity(body).build();
    }
    @Override
    public Response apisChangeLifecyclePost(String newState,String publishToGateway,String resubscription,String apiId,String ifMatch,String ifUnmodifiedSince){
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response apisCopyApiPost(String newVersion,String apiId){
        boolean isTenantFlowStarted = false;
        URI newVersionedApiUri = null;
        APIDTO newVersionedApi = null;

        try {
            APIIdentifier apiIdentifier = APIMappingUtil.getAPIIdentifier(apiId);
            String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
            APIProvider apiProvider = RestApiUtil.getProvider(loggedInUser);
            String tenantDomain =  CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            if(tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
            }

            org.wso2.carbon.apimgt.api.model.API api = apiProvider.getAPI(apiIdentifier);
            if (api != null) {
                apiProvider.createNewAPIVersion(api, newVersion);
                //get newly created API to return as response
                APIIdentifier apiNewVersionedIdentifier =
                    new APIIdentifier(apiIdentifier.getProviderName(), apiIdentifier.getApiName(), newVersion);
                newVersionedApi = APIMappingUtil.fromAPItoDTO(apiProvider.getAPI(apiNewVersionedIdentifier));
                //This was added in order to provide location header for HTTP response
                newVersionedApiUri =
                    new URI("http://10.100.7.39:9763/api/am/v1/apis/" +
                        apiIdentifier.getApiName() + "-" + newVersion + "-" + apiIdentifier.getProviderName());
            } else {
                throw new NotFoundException();
            }

        } catch (APIManagementException e) {
            throw new InternalServerErrorException(e);
        } catch (DuplicateAPIException e) {
            throw new InternalServerErrorException(e);
        } catch (URISyntaxException e) {
            throw new InternalServerErrorException(e);
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }

        return Response.created(newVersionedApiUri).entity(newVersionedApi).build();
    }
    @Override
    public Response apisApiIdGet(String apiId,String accept,String ifNoneMatch,String ifModifiedSince){
        boolean isTenantFlowStarted = false;
        APIDTO apiToReturn = new APIDTO();
        try {
            APIIdentifier apiIdentifier = APIMappingUtil.getAPIIdentifier(apiId);
            String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
            APIProvider apiProvider = RestApiUtil.getProvider(loggedInUser);
            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

            if (tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
            }
            org.wso2.carbon.apimgt.api.model.API api = apiProvider.getAPI(apiIdentifier);
            if (api != null) {
                apiToReturn = APIMappingUtil.fromAPItoDTO(api);
            } else {
                throw new NotFoundException();
            }
        } catch (APIManagementException e) {
            throw new InternalServerErrorException(e);
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }

        return Response.ok().entity(apiToReturn).build();
    }
    @Override
    public Response apisApiIdPut(String apiId,APIDTO body,String contentType,String ifMatch,String ifUnmodifiedSince){
        boolean isTenantFlowStarted = false;
        //validate ID of api dto with the provided id
        try {
            APIIdentifier apiIdentifier = APIMappingUtil.getAPIIdentifier(apiId);
            body.setName(apiIdentifier.getApiName());
            body.setVersion(apiIdentifier.getVersion());
            body.setProvider(APIUtil.replaceEmailDomainBack(apiIdentifier.getProviderName()));
            org.wso2.carbon.apimgt.api.model.API apiToAdd = APIMappingUtil.fromDTOtoAPI(body);

            String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
            APIProvider apiProvider = RestApiUtil.getProvider(loggedInUser);
            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            if(tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)){
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
            }
            apiProvider.updateAPI(apiToAdd);
        } catch (APIManagementException e) {
            throw new InternalServerErrorException(e);
        } catch (FaultGatewaysException e) {
            throw new InternalServerErrorException(e);
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }

        return Response.ok().entity(body).build();
    }
    @Override
    public Response apisApiIdDelete(String apiId,String ifMatch,String ifUnmodifiedSince){
        APIIdentifier apiIdentifier = APIMappingUtil.getAPIIdentifier(apiId);
        boolean isTenantFlowStarted = false;
        try{
            String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
            APIProvider apiProvider = RestApiUtil.getProvider(loggedInUser);
            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            if(tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
            }
            apiProvider.deleteAPI(apiIdentifier);
            KeyManager keyManager = KeyManagerHolder.getKeyManagerInstance();

            if (apiId.toString() != null) {
                keyManager.deleteRegisteredResourceByAPIId(apiId.toString());
            }

        } catch (APIManagementException e) {
            throw new InternalServerErrorException(e);
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
        return Response.ok().build();
    }
    @Override
    public Response apisApiIdDocumentsGet(String apiId,String limit,String offset,String query,String accept,String ifNoneMatch){
        List<DocumentDTO> list = new ArrayList<DocumentDTO>();
        try {
            String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
            APIProvider apiProvider = RestApiUtil.getProvider(loggedInUser);
            List<Documentation> docs = apiProvider.getAllDocumentation(APIMappingUtil.getAPIIdentifier(apiId));
            for (org.wso2.carbon.apimgt.api.model.Documentation temp : docs) {
                list.add(APIMappingUtil.fromDocumentationtoDTO(temp));
            }
        } catch (APIManagementException e) {
            throw new InternalServerErrorException(e);
        }
        return Response.ok().entity(list).build();
    }
    @Override
    public Response apisApiIdDocumentsPost(String apiId,DocumentDTO body,String contentType){
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }

    @Override
    public Response apisApiIdDocumentsDocumentIdGet(String apiId,String documentId,String accept,String ifNoneMatch,String ifModifiedSince){
        Documentation doc;
        try {
            String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
            APIProvider apiProvider = RestApiUtil.getProvider(loggedInUser);
            doc = apiProvider.getDocumentation(documentId);
            if(null != doc){
                return Response.ok().entity(doc).build();
            }
            else{
                throw new org.wso2.carbon.apimgt.rest.api.exception.NotFoundException();
            }
        } catch (APIManagementException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @Override
    public Response apisApiIdDocumentsDocumentIdPut(String apiId,String documentId,DocumentDTO body,String contentType,String ifMatch,String ifUnmodifiedSince){
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response apisApiIdDocumentsDocumentIdDelete(String apiId,String documentId,String ifMatch,String ifUnmodifiedSince){
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response apisApiIdEnvironmentsGet(String apiId,String limit,String offset,String query,String accept,String ifNoneMatch){
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response apisApiIdExternalStoresGet(String apiId,String limit,String offset,String query,String accept,String ifNoneMatch){
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
}