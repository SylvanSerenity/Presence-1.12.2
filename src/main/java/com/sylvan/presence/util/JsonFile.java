package com.sylvan.presence.util;

import com.google.gson.*;
import com.sylvan.presence.Presence;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class JsonFile {
	private final String fileName;
	private final File file;

	public JsonFile(final String fileName) {
		this.fileName = fileName;
		file = new File(fileName);

		// Create file if it doesn't exist
		if (!file.exists()) {
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				Presence.LOGGER.error("Failed to create configuration file for \"" + fileName + "\".", e);
			}
		}
	}

	public <T> JsonElement getOrSetValue(final String key, final T defaultValue) {
		try (final FileReader fileReader = new FileReader(file)) {
			final JsonElement root = new JsonParser().parse(fileReader);
			if (root.isJsonObject()) {
				final JsonObject object = root.getAsJsonObject();
				// Get the value if it exists
				if (object.has(key)) {
					try {
						return object.get(key);
					} catch (JsonSyntaxException e) {
						// Remove key on invalid input and have it reset to default
						Presence.LOGGER.error("JSON syntax exception for \"" + key + "\" in \"" + fileName + "\".", e);
						object.remove(key);
					}
				}
				return setValue(object, key, defaultValue);
			}
		} catch (IOException e) {
			Presence.LOGGER.error("IOException while attempting to get/set JSON value for \"" + key + "\" in \"" + fileName + "\".", e);
		} catch (JsonSyntaxException e) {
			Presence.LOGGER.error("JSON syntax exception for \"" + key + "\" in \"" + fileName + "\".", e);
		}
		return setValue(null, key, defaultValue);
	}

	public JsonObject getJsonObject() {
		try (final FileReader fileReader = new FileReader(file)) {
			final JsonElement root = new JsonParser().parse(fileReader);
			if (root.isJsonObject()) {
				return  root.getAsJsonObject();
			}
		} catch (IOException e) {
			Presence.LOGGER.error("IOException while attempting to get JSON object in \"" + fileName + "\".", e);
		} catch (JsonSyntaxException e) {
			Presence.LOGGER.error("JSON syntax exception in \"" + fileName + "\".", e);
		}
		return new JsonObject();
	}

	public <T> JsonElement setValue(JsonObject jsonObject, final String key, final T value) {
		final Gson gson = new Gson();
		if (jsonObject == null) {
			jsonObject = new JsonObject();
		}
		jsonObject.add(key, gson.toJsonTree(value));
		writeJson(jsonObject, true);
		return jsonObject.get(key);
	}

	public void writeJson(final JsonObject jsonObject, boolean pretty) {
		Gson gson;
		if (pretty) gson = new GsonBuilder().setPrettyPrinting().create();
		else gson = new GsonBuilder().create();

		try (final FileWriter fileWriter = new FileWriter(file)) {
			fileWriter.write(gson.toJson(jsonObject));
		} catch(IOException e) {
			Presence.LOGGER.error("Failed to save file \"" + fileName + "\".", e);
		}
	}

	public void wipe() {
		try {
			file.delete();
			file.createNewFile();
		} catch (IOException e) {
			Presence.LOGGER.error("Failed to clear configuration file for \"" + fileName + "\".", e);
		}
	}

	public boolean exists() {
		return file.exists();
	}
}
