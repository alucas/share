/**
 * Document List Component: activity
 */
var _regexNodeRef = new RegExp(/^[^\:^ ]+\:\/\/[^\:^ ]+\/[^ ]+$/);

postActivity();

function isNodeRef(value)
{
   var result = false;
   try
   {
      result = _regexNodeRef.test(String(value));
   }
   catch (e)
   {
   }
   return result;
}

/* Posts to the activities service after a Document Library action */
function postActivity()
{
   var data = {};
   
   /*
    * Activity Type
    */
   var type = json.get("type");
   if (type == null || type.textValue() == null || type.textValue().length === 0)
   {
      status.setCode(status.STATUS_BAD_REQUEST, "Activity 'type' parameter missing when posting activity");
      return;
   }
   type = type.textValue();
   /*
    * Site
    */
   var siteId = json.get("site");
   if (siteId == null || siteId.textValue() == null || siteId.textValue().length === 0)
   {
      status.setCode(status.STATUS_BAD_REQUEST, "'site' parameter missing when posting activity");
      return;
   }
   siteId = siteId.textValue();
   var site = siteService.getSite(siteId);
   if (site == null)
   {
      status.setCode(status.STATUS_BAD_REQUEST, "'" + siteId + "' is not a valid site");
      return;
   }

   /*
    * Check for known nodeRef values
    */
   var nodeRef = null,
      parentNodeRef = null;
   
   if (json.has("nodeRef"))
   {
      nodeRef = json.get("nodeRef").textValue();
      if (!isNodeRef(nodeRef))
      {
         status.setCode(status.STATUS_BAD_REQUEST, "'" + nodeRef + "' is not a valid NodeRef");
         return;
      }
   }
   if (json.has("parentNodeRef"))
   {
      parentNodeRef = json.get("parentNodeRef").textValue();
      if (!isNodeRef(parentNodeRef))
      {
         status.setCode(status.STATUS_BAD_REQUEST, "'" + parentNodeRef + "' is not a valid parent NodeRef");
         return;
      }
   }

   var strParams = "";

   switch (String(type).toLowerCase())
   {
      case "file-created":
      case "file-added":
      case "file-updated":
      case "file-liked":
      case "file-previewed":
      case "file-downloaded":
      case "folder-liked":
      case "inline-edit":
         data.title = json.get("fileName").textValue();
         data.nodeRef = nodeRef;
         strParams = "?nodeRef=" + nodeRef;
         break;
      
      case "files-added":
      case "files-deleted":
      case "files-updated":
      case "folders-deleted":
         data.title = json.get("fileCount").asText();
         strParams = "?path=" + json.get("path").textValue();
         if (parentNodeRef != null)
         {
            data.parentNodeRef = parentNodeRef;
         }
         break;
      
      case "file-deleted":
      case "folder-added":
      case "folder-deleted":
         data.title = json.get("fileName").textValue();
         data.nodeRef = nodeRef;
         strParams = "?path=" + json.get("path").textValue();
         if (parentNodeRef != null)
         {
            data.parentNodeRef = parentNodeRef;
         }
         break;
      
      default:
         status.setCode(status.STATUS_BAD_REQUEST, "'" + type + "' is not a valid activity type");
         return;
   }
   
   try 
   {
      // Log to activity service
      data.page = json.get("page").asText() + strParams;
      activities.postActivity("org.alfresco.documentlibrary." + type, siteId, "documentlibrary", jsonUtils.toJSONString(data));
   }
   catch(e)
   {
      if (logger.isLoggingEnabled())
      {
         logger.log(e);
      }
   }

}
