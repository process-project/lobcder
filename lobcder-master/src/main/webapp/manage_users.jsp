<%-- 
    Document   : manage_storage_sites
    Created on : Oct 6, 2013, 4:51:01 PM
    Author     : S. Koulouzis
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body class="yui3-skin-sam">
        <div id="dtable"></div>

        <div class="ba_content">
            <button id="btnSave" class="yui3-button">Save Selections</button>
            <button id="btnInsert" class="yui3-button">Insert New Row</button>
            <button id="btnDelete" class="yui3-button">Delete Selections</button>
        </div>
<!--        <script src="http://yui.yahooapis.com/3.10.3/build/yui/yui-min.js"></script>

        <script type="text/javascript">
            YUI().use( 
            "datatable", 
            "datatype",  
            "datasource-io", 
            "datasource-xmlschema", 
            "datatable-datasource", 
            "datatable-scroll", 
            'gallery-datatable-editable', 
            'gallery-datatable-celleditor-popup',
            "gallery-datatable-checkbox-select",
            "datatable-scroll", "datatable-sort",  "datatable-mutable", "event-custom",
            "datatype", "cssfonts", "cssbutton",
            'gallery-datatable-paginator','gallery-paginator-view',
            "gallery-datatable-formatters",
            function (Y) {
               
               
                var ds = new Y.DataSource.IO({
                    source: "rest/users/"
                });
                
                
                var schema = {
                        resultListLocator: "users",
                        resultFields: [
                            {key:"id", locator:"*[local-name() ='id']"},
                            {key:"roles", locator:"*[local-name() ='roles']"},
                            {key:"token", locator:"*[local-name() ='token']"},
                            {key:"uname", locator:"*[local-name() ='uname']"}
                        ]
                    };

                
                ds.plug(Y.Plugin.DataSourceXMLSchema, {
                    schema: schema
                });
            
            
                var encr = { 0:'false', 1:'true'};
            
                var cols =
                    [
                    { key: 'id', label: 'id',editable:false},
                    { key: 'uname', label: 'User Name' },
                    { key: 'roles', label: 'Roles'},
                    { key: 'token', label: 'password'}
                ];
            
                var table = new Y.DataTable({
                    columns:	cols,
                    data:		ds,
                    checkboxSelectMode:   true,
                    sortable:  true,
                    editable:       true,
                    editOpenType:   'click',
                    defaultEditor:  'text',
                    primaryKeys: ['id']
                });
                
                
                table.plug(Y.Plugin.DataTableDataSource, {
                    datasource: ds,
                    initialRequest: "query?id=all&output=xml"
                });
                
                ds.after("response", function() {
                    table.render("#dtable");
                }); 
            
            
                table.after('cellEditorSave', function(o){
                    /*    td:  record:  colKey:   newVal:  prevVal:  editorName:     */
                    var msg = 'Editor: ' + o.editorName + ' saved newVal=' + o.newVal + ' oldVal=' + o.prevVal;
                    //                    Y.log(msg);
                });
                
                
                
                Y.one("#btnSave").on("click", function(){
                    //                    var recs = table.get('checkboxSelected');
                    //                    var dataMsg = "<storageSiteWrapperList>"
                    //                    Y.Array.each(recs,function (r) {
                    //                        if(r.tr) {
                    //                            if ( confirm("Are you sure you want to save this record ?\n"+ r.record.get('ID')+" : "+r.record.get('resourceURI')) === true ) {
                    //                                dataMsg += "<sites>"
                    //                                var recindx = table.data.indexOf(r.record);
                    //            
                    //                                dataMsg += "<cache>"
                    //                                dataMsg +=r.record.get('cache');
                    //                                dataMsg += "</cache>";
                    //                            
                    //                                dataMsg += "<credential><storageSitePassword>";
                    //                                dataMsg +=r.record.get('password');
                    //                                dataMsg +="</storageSitePassword>";
                    //                                dataMsg +="<storageSiteUsername>";
                    //                                dataMsg += r.record.get('username');
                    //                                dataMsg +="</storageSiteUsername>";
                    //                                dataMsg += "</credential>";
                    //                            
                    //                                dataMsg +="<currentNum>";
                    //                                dataMsg +=r.record.get('currentNum');
                    //                                dataMsg +="</currentNum>";
                    //                                
                    //                                dataMsg +="<currentSize>";
                    //                                dataMsg +=r.record.get('currentSize');
                    //                                dataMsg +="</currentSize>";
                    //                                                            
                    //                                dataMsg +="<encrypt>";
                    //                                dataMsg += r.record.get('encrypted');
                    //                                dataMsg+="</encrypt>";
                    //                                
                    //                                dataMsg +="<quotaNum>";
                    //                                dataMsg += r.record.get('quotaNum');
                    //                                dataMsg+="</quotaNum>";
                    //                                    
                    //                                dataMsg +="<quotaSize>";
                    //                                dataMsg += r.record.get('quotaSize');
                    //                                dataMsg+="</quotaSize>";
                    //                            
                    //                                dataMsg +="<resourceURI>";
                    //                                dataMsg += r.record.get('resourceURI');
                    //                                dataMsg+="</resourceURI>";
                    //                                
                    //                                dataMsg +="<storageSiteId>";
                    //                                dataMsg += r.record.get('storageSiteId');
                    //                                dataMsg+="</storageSiteId>";
                    //                              
                    //                                dataMsg += "</sites>"
                    //                            }
                    //                          
                    //                        }
                    //                    });
                    //                    dataMsg += "</storageSiteWrapperList>"
                    //                    Y.log("dataMsg "+dataMsg);
                    //                    var uriPUT = "rest/storage_sites/set";
                    //                    send(dataMsg,uriPUT);
                    //                    table.checkboxClearAll();
                });
                
                
                var send = function(dataMsg,uriPUT) {
                    
                    // Define a function to handle the response data.
                    function complete(id, o, args) {
                        //Y.log("complete. id: "+id);
                    };
                    
                    // Subscribe to event "io:complete", and pass an array
                    // as an argument to the event handler "complete", since
                    // "complete" is global.   At this point in the transaction
                    // lifecycle, success or failure is not yet known.
                    Y.on('io:complete', complete, Y, ['lorem', 'ipsum']);
                
                    var cfg = {
                        method: 'PUT',
                        data: dataMsg,
                        headers: {
                            'Content-Type': 'application/xml'
                        }
                    };
                    var requestPUT = Y.io(uriPUT,cfg);           
                    Y.log("request: "+requestPUT);
                    
                    
                    table.render("#dtable");
                    table.datasource.load();
                }
                
                Y.one("#btnDelete").on("click", function(){
                    //                    var recs = table.get('checkboxSelected');
                    //                    // returns array of objects {tr,record,pkvalues} 
                    //                    var dataMsg ="<idWrapperList>";
                    //                    Y.Array.each(recs,function (r) {
                    //                        if(r.tr) {
                    //                            var recindx = table.data.indexOf(r.record);                            
                    //                            var id = r.record.get('ID');
                    //                            Y.log("id "+id);
                    //                            if ( confirm("Are you sure you want to delete this record ?\n"+ r.record.get('ID')+" : "+r.record.get('resourceURI')) === true ) {
                    //                                table.removeRow(recindx);
                    //                                dataMsg += "<ids>";
                    //                                dataMsg += id;
                    //                                dataMsg += "</ids>";
                    //                            }
                    //                        }
                    //                    },table);
                    //                    
                    //                    dataMsg +="</idWrapperList>";
                    //                    
                    //                    var uriPUT = "rest/storage_sites/delete";
                    //                    send(dataMsg,uriPUT);
                    //                    
                    //                    table.checkboxClearAll();
                });
                
                
                
                
                
                Y.one("#btnInsert").on("click",function() {
                    Y.log("Insert new! "+ table.data.recordset);
                    
                    var max = -1;
                    var colIndex=0;
                    var record;
                    while ( record = table.data.item(colIndex++)) {
                        var id = record.get('id');
                        var number = parseInt(id);
                        Y.log("id: "+number);
                        if(number > max){
                            max = number;
                            Y.log("max= "+max);
                        }
                    }
                    Y.log("max= "+max);
                          
                    table.addRow( [{id:++max,uname:'uname',
                            username:'uname',roles:'role1,role2',token:'secret'}] );
                });
            });
        </script>-->
    </body>
</html>
