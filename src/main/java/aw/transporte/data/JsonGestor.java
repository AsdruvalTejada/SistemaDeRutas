package aw.transporte.data;

import aw.transporte.structure.Grafo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class JsonGestor {

    private static final String JSON_PATH = "src/main/resources/data/db_transporte.json";

    //Aquí llamamos el Json de Google
    private Gson gsonTool;

    public JsonGestor() {
        //setPrettyPrinting hace que el JSON no sea una sola línea infinita, sino que tenga saltos y se vea "bonito"
        this.gsonTool = new GsonBuilder().setPrettyPrinting().create();
    }

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