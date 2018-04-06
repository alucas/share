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
package org.alfresco.repo.jscript;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.alfresco.repo.jscript.app.CustomResponse;

import java.util.Map;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;

/**
 * Populates DocLib webscript response with custom metadata output
 *
 * @author mikeh
 */
public final class SlingshotDocLibCustomResponse extends BaseScopableProcessorExtension
{
    private Map<String, Object> customResponses;

    /**
     * Set the custom response beans
     *
     * @param customResponses
     */
    public void setCustomResponses(Map<String, Object> customResponses)
    {
        this.customResponses = customResponses;
    }

    /**
     * Returns a JSON string to be added to the DocLib webscript response.
     *
     * @return The JSON string
     */
    public String getJSON() throws JsonProcessingException
    {
        return AlfrescoDefaultObjectMapper.writeValueAsString(this.getJSONObj());
    }

    /**
     * Returns a JSON object to be added to the DocLib webscript response.
     *
     * @return The JSON object
     */
    protected ObjectNode getJSONObj() throws JsonProcessingException
    {
        ObjectNode json = AlfrescoDefaultObjectMapper.createObjectNode();
        for (Map.Entry<String, Object> entry : this.customResponses.entrySet())
        {
            String response = AlfrescoDefaultObjectMapper
                    .writeValueAsString(((CustomResponse) entry.getValue()).populate());
            json.set(entry.getKey(), response == null ? NullNode.getInstance(): TextNode.valueOf(response));
        }
        return json;
    }
}
