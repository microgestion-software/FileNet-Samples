package com.microgestion.samples.filenet;

import java.util.Iterator;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.security.auth.Subject;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.filenet.api.collection.DocumentSet;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Document;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.UserContext;

/**
 * @author diegomendoza Ejemplo de programa para la eliminación
 * del contenido de una carpeta y todas sus subcarpetas.
 */
public class FileNetClearFolderContent {
	private static Logger logger = Logger.getLogger("om.microgestion.samples.filenet");

	private ObjectStore objectstore = null;
	
	/**
	 * @param cmd 
	 * @throws Exception
	 */
	public FileNetClearFolderContent(CommandLine cmd) throws Exception{
		logger.entering(FileNetClearFolderContent.class.getName(), "FileNetClearFolderContent (CommandLine cmd)");
		// Parametros
		String opt_cpe_url		= cmd.getOptionValue("url");
		String opt_stanza		= cmd.getOptionValue("stanza");
		String opt_username		= cmd.getOptionValue("username");
		String opt_password		= cmd.getOptionValue("password");
		String opt_objectstore	= cmd.getOptionValue("objectstore");
		 // Content Platform Engine - Object Sore
		objectstore = connectAndFetchObjectStore(opt_username, opt_password, opt_stanza, opt_cpe_url, opt_objectstore);

		logger.exiting(FileNetClearFolderContent.class.getName(), "FileNetClearFolderContent (CommandLine cmd)");
	}	
	
	
	/**
	 * Elimina el contenido de una carpeta
	 * 
	 * @param folder
	 * @param removefolder
	 */
	public void removeAllFoldersAndContent(String folderPath, boolean removefolder){
		logger.entering(FileNetClearFolderContent.class.getName(), "removeAllFoldersAndContent");
		logger.info("Remove Folder = " + removefolder);
		Folder oFolder = Factory.Folder.fetchInstance(objectstore, folderPath, null);
		removeFolderContent(oFolder, "   ", removefolder);
		
		logger.exiting(FileNetClearFolderContent.class.getName(), "removeAllFoldersAndContent");
	}	
	
	/**
	 * Elimina el contenido de una carpeta (objeto)
	 * 
	 * @param oFolder
	 * @param space
	 * @param bDelete
	 */
	public void removeFolderContent(Folder oFolder, String space, boolean bDelete){
		logger.entering(FileNetClearFolderContent.class.getName(), "removeFolderContent");
		
		// Eliminación de Subcarpetas
		FolderSet oFolderSet = oFolder.get_SubFolders();
		@SuppressWarnings("unchecked")
		Iterator<Folder> it = (Iterator<Folder>) oFolderSet.iterator();
		while(it.hasNext()){
			Folder oSubFolder = it.next();
			logger.info("Preparando eliminación de carpeta: " + oSubFolder.get_FolderName());
			removeFolderContent(oSubFolder, space + "     ", bDelete);
        }
		// Eliminación de documentos
		removeFolderDocumentst(oFolder, space);
		// Eliminación de Carpeta
		logger.info("Eliminación de carpeta: " + space + oFolder.get_FolderName());
		if (bDelete){
			oFolder.delete();
			oFolder.save(RefreshMode.REFRESH);
		}
		logger.exiting(FileNetClearFolderContent.class.getName(), "removeFolderContent");		
	}
	
	/**
	 * Elimina el contenido de una carpeta (objeto)
	 * 
	 * @param oFolder
	 */
	@SuppressWarnings("rawtypes")
	public void removeFolderDocumentst(Folder oFolder, String space){
		logger.entering(FileNetClearFolderContent.class.getName(), "removeFolderDocumentst");

        Document doc;
		DocumentSet documents= oFolder.get_ContainedDocuments();
        Iterator it=documents.iterator();
        while(it.hasNext())
        {
            doc= (Document) it.next();
    		logger.finer("Carpeta:" + space + oFolder.get_FolderName() + " - Eliminación de documento: "+ doc.get_Name());
            doc.delete();
            doc.save(RefreshMode.REFRESH);
            
        }
		logger.exiting(FileNetClearFolderContent.class.getName(), "removeFolderDocumentst");		
	}
	
	/**
	 * Main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			FileHandler fh = new FileHandler("ClearRepository.log");
	        fh.setFormatter(new SimpleFormatter());
	        logger.addHandler(fh);
	        logger.setLevel(Level.ALL);
	        
			// create Options object
			Options options = new Options();
			options.addOption("username", true, "WebSphere / FileNet user name");
			options.addOption("password", true, "WebSphere / FileNet user password");
			options.addOption("stanza", true, "FileNet JAAS stanza");
			options.addOption("url", true, "URI de acceso a Content Platform Engine (CPE)");
			options.addOption("objectstore", true, "FileNet Object Store");
			options.addOption("folder", true, "Carpeta cuyo contenido se desea eliminar");
			options.addOption("removefolder", true, "Permite indicar que también se desea eliminar la carpeta especificada");

			CommandLineParser parser = new BasicParser();
			CommandLine cmd = parser.parse( options, args);
			if(	cmd.hasOption("username") 
				&& cmd.hasOption("password") 
				&& cmd.hasOption("stanza") 
				&& cmd.hasOption("url") 
				&& cmd.hasOption("objectstore") 
				&& cmd.hasOption("folder") 
				&& cmd.hasOption("removefolder")){
				FileNetClearFolderContent app = new FileNetClearFolderContent(cmd);
				
				app.removeAllFoldersAndContent(
						cmd.getOptionValue("folder"), 
						Boolean.valueOf(cmd.getOptionValue("removefolder")));
				logger.info("SE CONCLUYÓ EL PROCESO DE ELIMNACION");
				
			}else{
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "FileNet - Uitilidad para eliminación de carpetas y su contenido", options );
			}
		} catch (Exception e) {
			logger.throwing(FileNetClearFolderContent.class.getName(), "main", e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Establece una conexión con el servidor FileNet y retona una referencia al
	 * Object Store (Repositorio)
	 * 
	 * @param userName
	 * @param password
	 * @param stanza
	 * @param uri
	 * @param os_name
	 * @return
	 */
	public  ObjectStore connectAndFetchObjectStore(String userName, String password, String stanza, String uri, String os_name) {
		logger.entering(FileNetClearFolderContent.class.getName(), "connectAndFetchObjectStore");
		Connection con;
		Domain dom;
		UserContext uc = UserContext.get();

		logger.fine("CPE url: " + uri);
		con = Factory.Connection.getConnection(uri);

		logger.fine("Stanza: " + stanza);
		logger.fine("UserName: " + userName);
		logger.fine("Password: " + password);
		Subject sub = UserContext.createSubject(con, userName, password, stanza);
		uc.pushSubject(sub);
		dom = Factory.Domain.fetchInstance(con, null, null);
		ObjectStore os = Factory.ObjectStore.fetchInstance(dom, os_name, null);
		logger.exiting(FileNetClearFolderContent.class.getName(), "connectAndFetchObjectStore");
		return os;
	}
}
