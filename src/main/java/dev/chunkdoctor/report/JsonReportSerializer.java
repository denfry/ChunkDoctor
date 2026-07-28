package dev.chunkdoctor.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;

public final class JsonReportSerializer {
    private final Gson gson;

    public JsonReportSerializer(boolean pretty) {
        GsonBuilder builder = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeAdapter(Instant.class, new TypeAdapter<Instant>() {
                    @Override
                    public void write(JsonWriter out, Instant value) throws IOException {
                        out.value(value.toString());
                    }

                    @Override
                    public Instant read(JsonReader in) throws IOException {
                        return Instant.parse(in.nextString());
                    }
                });
        if (pretty) {
            builder.setPrettyPrinting();
        }
        this.gson = builder.create();
    }

    public String serialize(ReportDocument document) {
        return gson.toJson(document);
    }
}
