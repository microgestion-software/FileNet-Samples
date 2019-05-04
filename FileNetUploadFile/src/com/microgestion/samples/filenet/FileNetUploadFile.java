package com.microgestion.samples.filenet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import javax.security.auth.Subject;

import com.filenet.api.collection.ContentElementList;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Connection;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Document;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.ReferentialContainmentRelationship;
import com.filenet.api.util.UserContext;

/**
 * @author diegomendoza Ejemplo de conexión a FileNet para el upload de un
 *         archivo en una carpeta determinada.
 */
public class FileNetUploadFile {

	/**
	 * Main
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 * Propiedades
		 */
		String cep_url = "http://xxxxxx:48019/wsi/FNCEWS40MTOM"; // URL del servicio CEWS - Conexión via vpn a través de tunel ssh
		String jass_stanza = "FileNetP8WSI"; // Utilización de transporte CEWS (Content Engine Web Services)
		String ad_user = "xxx_Servicio"; // Nombre de usuario
		String ad_pass = "xxx01"; // Contraseña
		String fn_objectstore = "RepositorioGeneral"; // Nombre del Object Store
		String _tef_doc_class = "xxx"; // Nombre de la clase documental
		String _folder_path = "/xxx"; // Path a la carpeta FileNet donde se quiere almacenar el archivo
		String _file_path = "/Users/diegomendoza/temp/descarga.jpeg"; // Path local al archivo que se quiere importar

		/*
		 * Conexión y obtención del Object Store
		 */
		ObjectStore _objectStore = connectAndFetchObjectStore(ad_user, ad_pass, jass_stanza, cep_url, fn_objectstore);

		/*
		 * Creación del documento, seteo de metadata y adición de contenido (archivo)
		 */
		Document _new_doc = Factory.Document.createInstance(_objectStore, _tef_doc_class);
		// Metadata
		String _cupon_admin = "VISA";
		Integer _cupon_numero = new Integer(1234);
		_new_doc.getProperties().putValue("DocumentTitle", _cupon_admin + " - " + _cupon_numero.toString()); // Propiedad heredada de la clase Document
		_new_doc.getProperties().putValue("CuponFecha", new Date()); // CuponFecha Si DateTime
		_new_doc.getProperties().putValue("CuponImporte", new Double(15333.34)); // CuponImporte Si Float
		_new_doc.getProperties().putValue("TarjetaAdministradora", _cupon_admin); // TarjetaAdministradora String
		_new_doc.getProperties().putValue("TarjetaNumero", "1111222233334444"); // TarjetaNumero Si String
		_new_doc.getProperties().putValue("CuponNumero", _cupon_numero); // CuponNumero Si Integer
		_new_doc.getProperties().putValue("CuponCuota", 1); // CuponCuota Integer
		_new_doc.getProperties().putValue("AutorizacionCodigo", 5678); // AutorizacionCodigo Integer
		_new_doc.getProperties().putValue("ComercioNumero", 15633258); // ComercioNumero Si Integer
		_new_doc.getProperties().putValue("TarjetaTipo", "-"); // TarjetaTipo String
		_new_doc.getProperties().putValue("TiendaNombre", "xxx"); // TiendaNombre String
		_new_doc.getProperties().putValue("TiendaNumero", 7); // TiendaNumero Integer
		// Contenido (Archivo)
		File file = new File(_file_path);
		ContentElementList _new_content_el = createContentElements(file);
		_new_doc.set_ContentElements(_new_content_el);
		// Check-in
		_new_doc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
		// Graba (pero queda unfiled, osea no está asociado a ninguna carpeta)
		_new_doc.save(RefreshMode.NO_REFRESH);

		/*
		 * Dispositción del contenido subido (atchivo) en una carpeta (Creación de
		 * relaciones)
		 */
		// Obtiene carpeta
		Folder _tef_folder = Factory.Folder.fetchInstance(_objectStore, _folder_path, null);
		// Crear relación entre la carpeta y el documento
		ReferentialContainmentRelationship _rcr_tef;
		System.out.println(file.getName());
		_rcr_tef = _tef_folder.file(_new_doc, AutoUniqueName.AUTO_UNIQUE, "Importado por FileNetAccessCheck", DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
		// Graba la relación y el documento queda asociado a la carpeta
		_rcr_tef.save(RefreshMode.REFRESH);
	}

	/**
	 * Crea el objeto ContentElementList requerido para almacenar contenido en un
	 * documento
	 * 
	 * @param file
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static ContentElementList createContentElements(File file) {
		ContentElementList cel = null;
		if (createContentTransfer(file) != null) {
			cel = Factory.ContentElement.createList();
			ContentTransfer ctNew = createContentTransfer(file);
			cel.add(ctNew);
		}
		return cel;
	}

	/**
	 * @param file
	 *            Crea un objeto de la clase ContentTransfer a partir del archivo
	 *            pasado por parámetro
	 * @return
	 */
	public static ContentTransfer createContentTransfer(File file) {
		ContentTransfer ctNew = null;
		if (readDocContentFromFile(file) != null) {
			ctNew = Factory.ContentTransfer.createInstance();
			ByteArrayInputStream is = new ByteArrayInputStream(readDocContentFromFile(file));
			ctNew.setCaptureSource(is);
			ctNew.set_RetrievalName(file.getName());
		}
		return ctNew;
	}

	/**
	 * Lee el contenido desde un archivo y lo deposita en un array de bytes.
	 * 
	 * @param file
	 * @return
	 */
	public static byte[] readDocContentFromFile(File file) {
		FileInputStream is;
		byte[] b = null;
		int fileLength = (int) file.length();
		if (fileLength != 0) {
			try {
				is = new FileInputStream(file);
				b = new byte[fileLength];
				is.read(b);
				is.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return b;
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
	public static ObjectStore connectAndFetchObjectStore(String userName, String password, String stanza, String uri, String os_name) {
		Connection con;
		Domain dom;
		UserContext uc = UserContext.get();

		System.out.println("Uri: " + uri);
		con = Factory.Connection.getConnection(uri);

		System.out.println("Stanza: " + stanza);
		System.out.println("UserName: " + userName);
		System.out.println("Password: " + password);
		Subject sub = UserContext.createSubject(con, userName, password, stanza);
		uc.pushSubject(sub);
		dom = Factory.Domain.fetchInstance(con, null, null);
		ObjectStore os = Factory.ObjectStore.fetchInstance(dom, os_name, null);
		return os;
	}
}
