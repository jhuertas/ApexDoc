package org.salesforce.apexdoc;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileManager {
    FileOutputStream fos;
    DataOutputStream dos;
    String path;
    public String header;
    public String APEX_DOC_PATH = "";
    private static final String FILE_LIST     = "ApexDocFiles.txt";
    private static final String IGNORE_FILE_LIST     = "IgnoreClass.txt";

    public StringBuffer infoMessages;

    public FileManager() {
        infoMessages = new StringBuffer();
    }

    private static String escapeHTML(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public FileManager(String path) {
        infoMessages = new StringBuffer();

        if (path == null || path.trim().length() == 0)
            this.path = ".";
        else
            this.path = path;
    }

    private boolean createHTML(TreeMap<String, String> mapFNameToContent, IProgressMonitor monitor) {
        try {
            if (path.endsWith("/") || path.endsWith("\\")) {
                path += Constants.ROOT_DIRECTORY; // + "/" + fileName + ".html";
            } else {
                path += "/" + Constants.ROOT_DIRECTORY; // + "/" + fileName + ".html";
            }

            (new File(path)).mkdirs();

            for (String fileName : mapFNameToContent.keySet()) {
                String contents = mapFNameToContent.get(fileName);
                fileName = path + "/" + fileName + ".html";
                File file = new File(fileName);
                fos = new FileOutputStream(file);
                dos = new DataOutputStream(fos);
                dos.write(contents.getBytes());
                dos.close();
                fos.close();
                infoMessages.append(fileName + " Processed...\n");
                System.out.println(fileName + " Processed...");
                if (monitor != null)
                    monitor.worked(1);
            }
            copy(path);
            return true;
        } catch (Exception e) {

            e.printStackTrace();
        }

        return false;
    }

//    private String strLinkfromModel(ApexModel model, String strClassName, String hostedSourceURL) {
//        return "<a target='_blank' class='hostedSourceLink' href='" + hostedSourceURL + strClassName + ".cls#L"
//                + model.getInameLine() + "'>";
//    }
    
    private String strLinkfromModel(ApexModel model, String strClassName, String hostedSourceURL) {
        return "";
    }

    private String strHTMLScopingPanel() {
        String str = "<tr><td colspan='2' style='text-align: center; display:none;' >";
        str += "Show: ";

        for (int i = 0; i < ApexDoc.rgstrScope.length; i++) {
            str += "<input type='checkbox' checked='checked' id='cbx" + ApexDoc.rgstrScope[i] +
                    "' onclick='ToggleScope(\"" + ApexDoc.rgstrScope[i] + "\", this.checked );'>" +
                    ApexDoc.rgstrScope[i] + "</input>&nbsp;&nbsp;";
        }
        str += "</td></tr>";
        return str;
    }

    /********************************************************************************************
     * @description main routine that creates an HTML file for each class specified
     * @param mapGroupNameToClassGroup
     * @param cModels
     * @param projectDetail
     * @param homeContents
     * @param hostedSourceURL
     * @param monitor
     */
    private void makeFile(TreeMap<String, ClassGroup> mapGroupNameToClassGroup, ArrayList<ClassModel> cModels,
            String projectDetail, String homeContents, String hostedSourceURL, IProgressMonitor monitor) {
		String links = "";

//		String links = "<table width='100%'>";
//		links += strHTMLScopingPanel();
//		links += "<tr style='vertical-align:top;' >";
		//links += getPageLinks(mapGroupNameToClassGroup, cModels);

        if (homeContents != null && homeContents.trim().length() > 0) {
            homeContents =  "<h2 class='section-title'>Home</h2>" + homeContents;
            homeContents = Constants.getHeader(projectDetail) + homeContents + Constants.FOOTER;
        } else {
            homeContents = Constants.DEFAULT_HOME_CONTENTS;
            homeContents =   "<h2 class='section-title'>Home</h2>" + homeContents;
            homeContents = Constants.getHeader(projectDetail) + homeContents + Constants.FOOTER;
        }

        String fileName = "";
        TreeMap<String, String> mapFNameToContent = new TreeMap<String, String>();
        mapFNameToContent.put("index", homeContents);

        // create our Class Group content files
        createClassGroupContent(mapFNameToContent, links, projectDetail, mapGroupNameToClassGroup, cModels, monitor);
        
        
        ArrayList<String> ignoredClasses = null;
        try {
          BufferedReader reader = new BufferedReader(new FileReader(IGNORE_FILE_LIST));
          ignoredClasses = new ArrayList<String>();
          String line;
          while ((line = reader.readLine()) != null) {
        	  ignoredClasses.add(line.trim().toLowerCase());
          }
          reader.close();
        } catch (Exception e) {
        }
        

        for (ClassModel cModel : cModels) {
            String contents = links;
            contents += getClassModelLinks(mapGroupNameToClassGroup, cModel, ignoredClasses);

            if (cModel.getNameLine() != null && cModel.getNameLine().length() > 0) {
                fileName = cModel.getClassName();
                //contents += "<td class='contentTD'>";
				if ((null == ignoredClasses)
						|| ignoredClasses.contains(cModel.getClassName()
								.toLowerCase()) == false) {
					contents += htmlForClassModel(cModel, hostedSourceURL);
				}
                // deal with any nested classes
                for (ClassModel cmChild : cModel.getChildClassesSorted()) {
					if ((null == ignoredClasses)
							|| ignoredClasses
									.contains(cmChild.getClassName().toLowerCase()) == false) {
						 contents += "<p/>";
		                 contents += htmlForClassModel(cmChild, hostedSourceURL);
    				}
                   
                }

            } else {
                continue;
            }
            //contents += "</div>";

            contents = Constants.getHeader(projectDetail) + contents + Constants.FOOTER;
            mapFNameToContent.put(fileName, contents);
            if (monitor != null)
                monitor.worked(1);
        }
        createHTML(mapFNameToContent, monitor);
    }

    /*********************************************************************************************
     * @description creates the HTML for the provided class, including its
     *              property and methods
     * @param cModel
     * @param hostedSourceURL
     * @return html string
     */
    private String htmlForClassModel(ClassModel cModel, String hostedSourceURL) {
        String contents = "";
        contents += "<h2 id='" + cModel.getClassName().replace(".", "_") + "' class='section-title'>" +
                
                cModel.getClassName()  +
                " Class </h2>";

        contents += "<div class='classSignature'>" +
                escapeHTML(cModel.getNameLine()) + "</div>";
        contents += "<div class='classDetails'>" ;

        
        if (cModel.getDescription() != "")
            contents += escapeHTML(cModel.getDescription());
        if (cModel.getAuthor() != "")
            contents += "<br/><br/>" + escapeHTML(cModel.getAuthor());
        if (cModel.getDate() != "")
            contents += "<br/>" + escapeHTML(cModel.getDate());
        contents += "</div><p/>";

        if (cModel.getProperties().size() > 0) {
            // start Properties
            contents +=
                    "<h2 class='subsection-title'>Properties</h2>" +
                            "<div class='subsection-container'> " +
                            "<table class='properties' > ";

            for (PropertyModel prop : cModel.getPropertiesSorted()) {
                contents += "<tr class='propertyscope" + prop.getScope() + "'><td class='clsPropertyName'>" +
                        prop.getPropertyName() + "</td>";
                contents += "<td><div class='clsPropertyDeclaration'>" +
                        strLinkfromModel(prop, cModel.getTopmostClassName(), hostedSourceURL) +
                        escapeHTML(prop.getNameLine()) + "</a></div>";
                contents += "<div class='clsPropertyDescription'>" + escapeHTML(prop.getDescription()) + "</div></tr>";
            }
            // end Properties
            contents += "</table></div><p/>";
        }

        if (cModel.getMethods().size() > 0) {
            // start Methods
            contents +=
                    "<h2 class='subsection-title'>Methods</h2>" +
                            "<div class='subsection-container'> ";

            // method Table of Contents (TOC)
            contents += "<ul class='methodTOC'>";
            for (MethodModel method : cModel.getMethodsSorted()) {
                contents += "<li class='methodscope" + method.getScope() + "' >";
                contents += "<a class='methodTOCEntry' href='#" + method.getMethodName() + "'>"
                        + escapeHTML(method.getNameLineWithoutScope()) + "</a>";
                if (method.getDescription() != "")
                    contents += "<div class='methodTOCDescription'>" + method.getDescription() + "</div>";
                contents += "</li>";
            }
            contents += "</ul>";

            // full method display
            for (MethodModel method : cModel.getMethodsSorted()) {
                contents += "<div class='methodscope" + method.getScope() + "' >";
                contents += "<h2 id='" + method.getMethodName() + "' class='methodHeader'>"
                        + escapeHTML(method.getNameLineWithoutScope()) + "</h2>" +
                        "<div class='methodSignature'>" +
                        escapeHTML(method.getNameLine()) + "</div>";

                if (method.getDescription() != "")
                    contents += "<div class='methodDescription'>" + escapeHTML(method.getDescription()) + "</div>";

                if (method.getParams().size() > 0) {
                    contents += "<div class='methodSubTitle'>Parameters</div>";
                    for (String param : method.getParams()) {
                        param = escapeHTML(param);
                        if (param != null && param.trim().length() > 0) {
                            Pattern p = Pattern.compile("\\s");
                            Matcher m = p.matcher(param);

                            String paramType;
                            String paramName;
                            String paramDescription;
                            if (m.find()) {
                            	int ich = m.start();
                                paramType = param.substring(0, ich);
                                if(m.find()){
                                	int ich2 = m.start();
                                	paramName = param.substring(ich + 1, ich2);
                                    paramDescription = param.substring(ich2 + 1);
                                }else{
                                	paramName = "";                       
                                    paramDescription = param.substring(ich + 1);
                                }
                            } else {
                            	paramType = "";
                                paramName = param;
                                paramDescription = null;
                            }
                            contents += "<div><span class='paramType'>" + paramType + "</span><span class='paramName'>" + paramName + "</span></div>";

                            if (paramDescription != null)
                                contents += "<div class='paramDescription'>" + paramDescription + "</div>";
                        }
                    }
                    // end Parameters
                }

                if (method.getReturns() != "") {
                    contents += "<div class='methodSubTitle'>Return Value</div>";
                    contents += "<div class='methodReturns'>" + escapeHTML(method.getReturns()) + "</div>";
                }

                if (method.getExample() != "") {
                    contents += "<div class='methodSubTitle'>Example</div>";
                    contents += "<blockquote><pre><code class='methodExample'>" + escapeHTML(method.getExample()) + "</code></pre></blockquote>";
                }

                if (method.getAuthor() != "") {
                    contents += "<div class='methodSubTitle'>Author</div>";
                    contents += "<div class='methodReturns'>" + escapeHTML(method.getAuthor()) + "</div>";
                }

                if (method.getDate() != "") {
                    contents += "<div class='methodSubTitle'>Date</div>";
                    contents += "<div class='methodReturns'>" + escapeHTML(method.getDate()) + "</div>";
                }

                // end current method
                contents += "</div>";
            }
            // end all methods
            contents += "</div>";
        }

        return contents;
    }

    // create our Class Group content files
    private void createClassGroupContent(TreeMap<String, String> mapFNameToContent, String links, String projectDetail,
            TreeMap<String, ClassGroup> mapGroupNameToClassGroup,
            ArrayList<ClassModel> cModels, IProgressMonitor monitor) {

        for (String strGroup : mapGroupNameToClassGroup.keySet()) {
            ClassGroup cg = mapGroupNameToClassGroup.get(strGroup);
            if (cg.getContentSource() != null) {
                String cgContent = parseHTMLFile(cg.getContentSource());
                if (cgContent != "") {
                    String strHtml = Constants.getHeader(projectDetail) + links +
                            "<h2 class='section-title'>" +
                            escapeHTML(cg.getName()) + "</h2>" + cgContent;
                    strHtml += Constants.FOOTER;
                    mapFNameToContent.put(cg.getContentFilename(), strHtml);
                    if (monitor != null)
                        monitor.worked(1);
                }
            }
        }
    }

    /**********************************************************************************************************
     * @description generate the HTML string for the Class Menu to display on
     *              each page.
     * @param mapGroupNameToClassGroup
     *            map that holds all the Class names, and their respective Class
     *            Group.
     * @param cModels
     *            list of ClassModels
     * @return String of HTML
     */
    private String getClassModelLinks(TreeMap<String, ClassGroup> mapGroupNameToClassGroup, ClassModel cModel, ArrayList<String> ignoredClasses) {
        boolean createMiscellaneousGroup = false;

        // this is the only place we need the list of class models sorted by name.
        TreeMap<String, ClassModel> tm = new TreeMap<String, ClassModel>();
            tm.put(cModel.getClassName().toLowerCase(), cModel);
            if (!createMiscellaneousGroup && cModel.getClassGroup() == null)
                createMiscellaneousGroup = true;
        
        ArrayList<ClassModel> cModels  = new ArrayList<ClassModel>(tm.values());

        //String links = "<td width='20%' vertical-align='top' >";
        String links = "";

        links += "<div class='sidebar'><div class='navbar'><nav role='navigation'>";
        //links += "<li id='idMenuindex'><a href='.' onclick=\"gotomenu('index.html', event);return false;\" class='nav-item'>Home</a></li>";

        // add a bucket ClassGroup for all Classes without a ClassGroup specified
        if (createMiscellaneousGroup)
            mapGroupNameToClassGroup.put("API", new ClassGroup("API", null));

        // create a sorted list of ClassGroups

        for (String strGroup : mapGroupNameToClassGroup.keySet()) {
//            ClassGroup cg = mapGroupNameToClassGroup.get(strGroup);
//            String strGoTo = "onclick=\"gotomenu(document.location.href, event);return false;\"";
//            if (cg.getContentFilename() != null)
//                strGoTo = "onclick=\"gotomenu('" + cg.getContentFilename() + ".html" + "', event);return false;\"";
//            links += "<li class='header' id='idMenu" + cg.getContentFilename() +
//                    "'><a class='nav-item nav-section-title' href='.' " +
//                    strGoTo + " class='nav-item'>" + strGroup + "<span class='caret'></span></a></li>";
//            links += "<ul>";
//
//            // even though this algorithm is O(n^2), it was timed at just 12
//            // milliseconds, so not an issue!
                if (strGroup.equals(cModel.getClassGroup())
                        || (cModel.getClassGroup() == null && strGroup == "API")) {
                    if (cModel.getNameLine() != null && cModel.getNameLine().trim().length() > 0) {
                        String fileName = cModel.getClassName();
                        links += "<div class='mainsection-title classscope" + cModel.getScope() + "' id='idMenu" + fileName +
                                "'>" + 
                                fileName + " Classes </div>";
                        links += "<ul id='mynavbar'>";
                        for (ClassModel cmChild : cModel.getChildClassesSorted()) {
        					if ((null == ignoredClasses)
        							|| ignoredClasses
        									.contains(cmChild.getClassName().toLowerCase()) == false) {
        						String childName = cmChild.getClassName();
                                links += "<li class='subitem classscope" + cModel.getScope() + "' id='idMenu" + childName +
                                        "'><a href='#" + childName.replace(".", "_") + "' class='nav-item sub-nav-item scope" +
                                        cModel.getScope() + "'>" +
                                        childName + "</a></li>";
            				}
                           
                        }
                        links += "</ul>";
                    }
                }
            
//
         //   links += "</ul>";
       }

        links += "</nav></div></div></div>";

//        links += "</td>";
        return links;
    }

    private void docopy(String source, String target) throws Exception {

        InputStream is = this.getClass().getResourceAsStream(source);
        // InputStreamReader isr = new InputStreamReader(is);
        // BufferedReader reader = new BufferedReader(isr);
        FileOutputStream to = new FileOutputStream(target + "/" + source);

        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = is.read(buffer)) != -1) {
            to.write(buffer, 0, bytesRead); // write
        }

        to.flush();
        to.close();
        is.close();
    }

    private void copy(String toFileName) throws IOException, Exception {
        //docopy("apex_doc_logo.png", toFileName);
        //docopy("ApexDoc.css", toFileName);
        //docopy("ApexDoc.js", toFileName);
        //docopy("CollapsibleList.js", toFileName);
        //docopy("jquery-1.11.1.js", toFileName);
        //docopy("toggle_block_btm.gif", toFileName);
        //docopy("toggle_block_stretch.gif", toFileName);

    }

    public ArrayList<File> getFiles(String path) {
        File folder = new File(path);
        ArrayList<File> listOfFilesToCopy = new ArrayList<File>();
        if (folder != null) {
            File[] listOfFiles = folder.listFiles();
                  
            ArrayList<String> includedFiles = null;
            try {
              BufferedReader reader = new BufferedReader(new FileReader(FILE_LIST));
              includedFiles = new ArrayList<String>();
              String line;
              while ((line = reader.readLine()) != null) {
                includedFiles.add(line.trim().toLowerCase());
              }
              reader.close();
            } catch (Exception e) {
            }
            
            ArrayList<File> files = new ArrayList<File>();
			if (listOfFiles != null) {
				for (File f : listOfFiles) {
					if ((null == includedFiles)
							|| includedFiles
									.contains(f.getName().toLowerCase())) {
						files.add(f);
					}
				}
			}
            
            if (files != null && files.size() > 0) {
                return files;
            } else {
                System.out.println("WARNING: No files found in directory: " + path);
            }
        }
        return listOfFilesToCopy;
    }

    public void createDoc(TreeMap<String, ClassGroup> mapGroupNameToClassGroup, ArrayList<ClassModel> cModels,
            String projectDetail, String homeContents, String hostedSourceURL, IProgressMonitor monitor) {
        makeFile(mapGroupNameToClassGroup, cModels, projectDetail, homeContents, hostedSourceURL, monitor);
    }

    private String parseFile(String filePath) {
        try {
            if (filePath != null && filePath.trim().length() > 0) {
                FileInputStream fstream = new FileInputStream(filePath);
                // Get the object of DataInputStream
                DataInputStream in = new DataInputStream(fstream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String contents = "";
                String strLine;

                while ((strLine = br.readLine()) != null) {
                    // Print the content on the console
                    strLine = strLine.trim();
                    if (strLine != null && strLine.length() > 0) {
                        contents += strLine;
                    }
                }
                // System.out.println("Contents = " + contents);
                br.close();
                return contents;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public String parseHTMLFile(String filePath) {

        String contents = (parseFile(filePath)).trim();
        if (contents != null && contents.length() > 0) {
            int startIndex = contents.indexOf("<body>");
            int endIndex = contents.indexOf("</body>");
            if (startIndex != -1) {
                if (contents.indexOf("</body>") != -1) {
                    contents = contents.substring(startIndex, endIndex);
                    return contents;
                }
            }
        }
        return "";
    }

}
