[#ftl]
[#import "pluginInfoLibrary.ftl" as lib/]
<html>
<head>
    <title>Plugin info</title>
    <link type="text/css" rel="stylesheet" href="../css/admin.css"/>
    <script type="text/javascript" src="../javascript/lib/jquery/jquery-1.7.2.min.js"></script>
    <script type="text/javascript">

        var lastKey = 0;

        function showDetails(key) {
            hideDetails(lastKey);
            $("#details-" + key).show();
            lastKey = key;
        }

        function hideDetails(key) {
            $("#details-" + key).hide();
            lastKey = 0;
        }

    </script>

    <style type="text/css">

        .infoBox {
            padding: 8px;
            margin: 10px;
            border: 1px dotted #000000;
            background-color: #EEEEEE;
        }

        pre {
            font-size: 10pt;
            border-left: 1px #000000 dotted;
            padding: 10px;
        }

        .detailWindow {
            background-color: #d3d3d3;
            border: 2px solid gray;
            padding: 5px;
            position: fixed;
            width: 50%;
            max-height: 60%;
            overflow-y: auto;
            overflow-x: auto;
            top: 10px;
            right: 10px;
            display: none;
            z-index: 999;
        }
    </style>

</head>
<body>
<h1>Admin / System / Plugin Info</h1>

<div class="infoBox">
    <b>Registered Plugins</b>

    <ul>
    [#list pluginHandles as plugin ]
            [@lib.pluginInfoRowWithDetails plugin=plugin/]
		[/#list]
    </ul>
</div>

<div class="infoBox">
    <b>Registered Extensions</b>

[#list extMap?keys as extName]
    [#assign extList = extMap[extName]]
    [#if extList?size > 0]
        <fieldset class="infoBox">
            <legend>${extName} Extensions <small>(${extList?size})</small></legend>
            <ul>
                [#list extList as html]
                    <li>${html}</li>
                [/#list]
            </ul>
        </fieldset>
    [/#if]
[/#list]
</div>

[#list pluginHandles as plugin]
    [@lib.pluginDetail plugin=plugin/]
[/#list]

</body>
</html>
