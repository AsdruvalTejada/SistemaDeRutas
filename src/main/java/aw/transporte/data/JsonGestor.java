package aw.transporte.data;

import aw.transporte.structure.Grafo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Clase: JsonGestor
 * Objetivo: Motor de serialización principal del sistema. Se encarga de transformar
 * la estructura compleja del Grafo (Paradas y Rutas) a texto JSON y viceversa,
 * garantizando la persistencia de la topología de la red de transporte.
 */
public class JsonGestor {

    private static final String JSON_PATH = "src/main/resources/data/db_transporte.json";

    //Aquí llamamos el Json de Google
    private Gson gsonTool;

    public JsonGestor() {
        //setPrettyPrinting hace que el JSON no sea una sola línea infinita, sino que tenga saltos y se vea "bonito"
        this.gsonTool = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Función: saveGrafo
     * Objetivo: Serializar el objeto Grafo y escribirlo en el archivo de base de datos,
     * creando los directorios necesarios si estos no existen.
     * @param mainGrafo (Grafo) La red de transporte actual en memoria.
     */
    // Método para guardar
    public void saveGrafo(Grafo mainGrafo) {
        File docFile = new File(JSON_PATH);

        //Esto verifica si las carpetas "resources/data" existen. Si no, las crea al instante.
        if (docFile.getParentFile() != null) {
            //noinspection ResultOfMethodCallIgnored
            docFile.getParentFile().mkdirs();
        }

        try (FileWriter fWriter = new FileWriter(docFile)) {
            // Convierte tu objeto Grafo a texto y lo escribe
            gsonTool.toJson(mainGrafo, fWriter);
            System.out.println("Data guardada con éxito en el JSON.");
        } catch (IOException ex) {
            System.out.println("Error al guardar la data: " + ex.getMessage());
        }
    }

    /**
     * Función: fetchGrafoData
     * Objetivo: Buscar, leer y deserializar el archivo JSON para reconstruir el Grafo completo
     * en memoria al iniciar la aplicación.
     * @return (Grafo) El grafo cargado desde disco, o un grafo vacío si es la primera ejecución.
     */
    // Método para cargar
    public Grafo fetchGrafoData() {
        File docFile = new File(JSON_PATH);

        //Verificamos si el archivo ya existe antes de intentar leerlo
        if (docFile.exists()) {
            try (FileReader fReader = new FileReader(docFile)) {
                //Reconstruye el objeto Grafo a partir del texto JSON
                Grafo loadedGrafo = gsonTool.fromJson(fReader, Grafo.class);
                System.out.println("Grafo cargado. Paradas recuperadas: " + loadedGrafo.getParadas().size());
                return loadedGrafo;
            } catch (IOException ex) {
                System.out.println("Fallo al hacer fetch del JSON: " + ex.getMessage());
            }
        }

        // Si no existe (primer run del programa), devolvemos un grafo vacío
        System.out.println("No hay DB previa. Iniciando un un nuevo grafo.");
        return new Grafo();
    }
}