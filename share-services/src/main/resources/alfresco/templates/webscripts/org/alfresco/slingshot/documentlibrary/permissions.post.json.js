<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary/parse-args.lib.js">

/**
 * Entry point for rmpermissions POST data webscript.
 * Applies supplied permissions to a node.
 * 
 * @method main
 */
function main()
{
   /**
    * nodeRef input: store_type, store_id and id
    */
   var storeType = url.templateArgs.store_type,
      storeId = url.templateArgs.store_id,
      id = url.templateArgs.id,
      nodeRef = storeType + "://" + storeId + "/" + id,
      node = ParseArgs.resolveNode(nodeRef);
   
   if (node == null)
   {
      node = search.findNode(nodeRef);
      if (node === null)
      {
         status.setCode(status.STATUS_NOT_FOUND, "Not a valid nodeRef: '" + nodeRef + "'");
         return null;
      }
   }

   var location = Common.getLocation (node);
   var siteManagerAuthority = "GROUP_site_" + location.site + "_SiteManager";
      
   if (json.has("permissions") == false)
   {
      status.setCode(status.STATUS_BAD_REQUEST, "Permissions value missing from request.");
   }
   
   // Inherited permissions flag
   // First set inherit and then modify permissions
   // See MNT-11725
   if (json.has("isInherited"))
   {
      var isInherited = json.get("isInherited").booleanValue();
      if (location.site != null)
      {
         if (isInherited == false)
         {
            // Insure Site Managers can still manage content.
            node.setPermission("SiteManager", siteManagerAuthority);
         }
      }
      node.setInheritsPermissions(isInherited);
   }
   
   var permissions = json.get("permissions");
   var isInherited = json.get("isInherited").booleanValue();
   for (var i = 0; i < permissions.size(); i++)
   {
      var perm = permissions.get(i);
      
      // collect values for the permission setting
      var authority = perm.get("authority").textValue();
      
      var role = perm.get("role").textValue();
      var remove = false;
      if (perm.has("remove"))
      {
         remove = perm.get("remove").booleanValue();
      }
      
      // Apply or remove permission
      if (remove)
      {
         // Prevent the removal of the SiteManager group authority
         if (!(role == "SiteManager" && authority == siteManagerAuthority && !isInherited))
         {
            node.removePermission(role, authority);
         }
      }
      else
      {
         var isSpecialAuthority = false;
         switch ("" + authority)
         {
            case "GROUP_EVERYONE":
            case "ROLE_ADMINISTRATOR":
            case "ROLE_GUEST":
            case "ROLE_OWNER":
               isSpecialAuthority = true;
               break;
         }

         if (!isSpecialAuthority && people.getGroup(authority) == null && people.getPerson(authority) == null)
         {
            // ACE-3280: silently not add non-existent users
            return;
         }

         node.setPermission(role, authority);
      }
   }
}

main();