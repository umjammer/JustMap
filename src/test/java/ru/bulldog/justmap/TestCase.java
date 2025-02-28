/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package ru.bulldog.justmap;

import javax.imageio.ImageIO;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.util.Base64;
import org.apache.commons.io.HexDump;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static java.lang.System.getLogger;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-03-01 nsano initial version <br>
 */
class TestCase {

    private static final Logger logger = getLogger(TestCase.class.getName());

    @Test
    @EnabledIfEnvironmentVariable(named = "uuid", matches = ".*")
    void test01() throws Exception {
        Gson gson = new GsonBuilder().create();
        String uuid = System.getenv("uuid");
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/%s".formatted(uuid);
        String json = new String(URI.create(url).toURL().openStream().readAllBytes());
logger.log(Level.INFO, json);
        JsonObject map = gson.fromJson(json, JsonObject.class);
logger.log(Level.INFO, map);
        String b64 = ((JsonObject) ((JsonArray) map.get("properties")).get(0)).get("value").getAsString();
logger.log(Level.INFO, b64);
HexDump.dump(Base64.getDecoder().decode(b64), System.err);
        String json2 = new String(Base64.getDecoder().decode(b64));
        JsonObject map2 = gson.fromJson(json2, JsonObject.class);
        String url2 = ((JsonObject) ((JsonObject) map2.get("textures")).get("SKIN")).get("url").getAsString();
logger.log(Level.INFO, url2);
        BufferedImage image = ImageIO.read(URI.create(url2).toURL());
logger.log(Level.INFO, image);
    }
}
