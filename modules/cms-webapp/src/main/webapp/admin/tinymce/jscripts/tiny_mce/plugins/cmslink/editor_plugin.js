(function(){tinymce.PluginManager.requireLangPack("cmslink");tinymce.create("tinymce.plugins.CMSLinkPlugin",{init:function(e,t){e.addCommand("cmslink",function(){var n=e.selection;var r=460,i=300;e.windowManager.open({file:"adminpage?page=1048&op=select",width:r+parseInt(e.getLang("cmslink.delta_width",0)),height:i+parseInt(e.getLang("cmslink.delta_height",0)),inline:1},{plugin_url:t})});e.addButton("cmslink",{title:"advlink.link_desc",cmd:"cmslink"});e.addShortcut("ctrl+k","cmslink.cmslink_desc","cmslink");e.onNodeChange.add(function(e,t,n,r){var i=n.nodeName=="A"&&!n.name||n.nodeName=="IMG"&&!n.name&&n.parentNode.nodeName=="A";t.setActive("cmslink",i)})},getInfo:function(){return{longname:"CMS Link",author:"Enonic AS",authorurl:"http://www.enonic.com",infourl:"http://www.enonic.com",version:"0.3"}}});tinymce.PluginManager.add("cmslink",tinymce.plugins.CMSLinkPlugin)})()