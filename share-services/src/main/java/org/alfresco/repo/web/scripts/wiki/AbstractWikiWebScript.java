/*
 * #%L
 * Alfresco Share Services AMP
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.repo.web.scripts.wiki;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.query.PagingRequest;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.activities.ActivityService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.wiki.WikiPageInfo;
import org.alfresco.service.cmr.wiki.WikiService;
import org.alfresco.util.ScriptPagingDetails;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.surf.util.URLEncoder;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * @author Nick Burch
 * @since 4.0
 */
public abstract class AbstractWikiWebScript extends DeclarativeWebScript
{
    public static final String WIKI_SERVICE_ACTIVITY_APP_NAME = "wiki";
    
    /**
     * When no maximum or paging info is given, what should we use?
     */
    protected static final int MAX_QUERY_ENTRY_COUNT = 1000;
    
    private static Log logger = LogFactory.getLog(AbstractWikiWebScript.class);
    
    // Injected services
    protected NodeService nodeService;
    protected SiteService siteService;
    protected WikiService wikiService;
    protected PersonService personService;
    protected ActivityService activityService;
    
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    public void setSiteService(SiteService siteService)
    {
        this.siteService = siteService;
    }
    
    public void setWikiService(WikiService wikiService)
    {
        this.wikiService = wikiService;
    }
    
    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }
    
    public void setActivityService(ActivityService activityService)
    {
        this.activityService = activityService;
    }
    
    /**
     * Builds up a listing Paging request, based on the arguments
     *  specified in the URL
     */
    protected PagingRequest buildPagingRequest(WebScriptRequest req)
    {
       return new ScriptPagingDetails(req, MAX_QUERY_ENTRY_COUNT);
    }
    
    protected void addActivityEntry(String event, WikiPageInfo wikiPage, SiteInfo site, 
            WebScriptRequest req, JsonNode json)
    {
        addActivityEntry(event, wikiPage, site, req, json, Collections.<String, String>emptyMap());
    }
    
    /**
     * Generates an activity entry for the link
     * 
     * @param event    a String representing the event.
     * @param wikiPage the wiki page generating the activity.
     * @param site     the site in which the wiki page was created.
     * @param req      the {@link WebScriptRequest}.
     * @param additionalData any additional data required for the activity.
     */
    protected void addActivityEntry(String event,
            WikiPageInfo wikiPage, SiteInfo site, 
            WebScriptRequest req, JsonNode json,
            Map<String, String> additionalData)
    {
       // What page is this for?
       String page = req.getParameter("page");
       if (page == null && json != null)
       {
          if (json.has("page"))
          {
             page = json.get("page").textValue();
          }
       }
       if (page == null)
       {
          // Default
          page = "wiki";
       }
       
       try
       {
           ObjectNode activityJson = AlfrescoDefaultObjectMapper.createObjectNode();
           activityJson.put("title", wikiPage.getTitle());
           activityJson.put("page", page + "?title=" + URLEncoder.encodeUriComponent(wikiPage.getTitle()));
           for (Map.Entry<String, String> entry : additionalData.entrySet())
           {
               activityJson.put(entry.getKey(), entry.getValue());
           }

           activityService.postActivity(
                "org.alfresco.wiki.page-" + event,
                site.getShortName(),
                WIKI_SERVICE_ACTIVITY_APP_NAME,
                AlfrescoDefaultObjectMapper.writeValueAsString(activityJson));
       }
       catch (Exception e)
       {
          // Warn, but carry on
          logger.warn("Error adding wiki page " + event + " to activities feed", e);
       }
    }
    
    protected NodeRef personForModel(String username)
    {
       if (username == null || username.isEmpty())
       {
          return null;
       }
       
       try
       {
          // Will turn into a Script Node needed of the person
           return personService.getPerson(username);
       }
       catch(NoSuchPersonException e)
       {
          // This is normally caused by the person having been deleted
          return null;
       }
    }
    
    protected Map<String, Object> renderWikiPage(WikiPageInfo page)
    {
       Map<String, Object> res = new HashMap<>();
       res.put("page", page);
       res.put("node", page.getNodeRef());
       res.put("name", page.getSystemName());
       res.put("title", page.getTitle());
       res.put("contents", page.getContents());
       res.put("tags", page.getTags());
       
       // Both forms used for dates
       res.put("createdOn", page.getCreatedAt());
       res.put("modifiedOn", page.getModifiedAt());
       res.put("created", page.getCreatedAt());
       res.put("modified", page.getModifiedAt());
       
       // For most things, we want blank instead of null
       for (Map.Entry<String, Object> entry : res.entrySet())
       {
           if (entry.getValue() == null) entry.setValue("");
       }

       // FTL needs a script node of the people, or null if unavailable
       res.put("createdBy", personForModel(page.getCreator()));
       res.put("modifiedBy", personForModel(page.getModifier()));
       
       // All done
       return res;
    }
    
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req,
          Status status, Cache cache) 
    {
       Map<String, String> templateVars = req.getServiceMatch().getTemplateVars();
       if (templateVars == null)
       {
           throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No parameters supplied");
       }
       
       
       // Parse the JSON, if supplied
       JsonNode json = null;
       String contentType = req.getContentType();
       if (contentType != null && contentType.indexOf(';') != -1)
       {
          contentType = contentType.substring(0, contentType.indexOf(';'));
       }
       if (MimetypeMap.MIMETYPE_JSON.equals(contentType))
       {
          try
          {
             json = AlfrescoDefaultObjectMapper.getReader().readTree(req.getContent().getContent());
          }
          catch (IOException io)
          {
             throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid JSON: " + io.getMessage());
          }
       }
       
       
       // Get the site short name. Try quite hard to do so...
       String siteName = templateVars.get("siteId");
       if (siteName == null)
       {
          siteName = req.getParameter("site");
       }
       if (siteName == null && json != null)
       {
          if (json.has("siteid"))
          {
             siteName = json.get("siteid").textValue();
          }
          else if (json.has("siteId"))
          {
             siteName = json.get("siteId").textValue();
          }
          else if(json.has("site"))
          {
             siteName = json.get("site").textValue();
          }
       }
       if (siteName == null)
       {
           throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No site given");
       }
       
       // Grab the requested site
       SiteInfo site = siteService.getSite(siteName);
       if (site == null)
       {
          String error = "Could not find site: " + siteName;
          throw new WebScriptException(Status.STATUS_NOT_FOUND, error);
       }
       
       String pageTitle = templateVars.get("pageTitle");
       
       // Have the real work done
       return executeImpl(site, pageTitle, req, json, status, cache); 
    }
    
    protected abstract Map<String, Object> executeImpl(SiteInfo site, 
          String pageTitle, WebScriptRequest req, JsonNode json,
          Status status, Cache cache);
}
