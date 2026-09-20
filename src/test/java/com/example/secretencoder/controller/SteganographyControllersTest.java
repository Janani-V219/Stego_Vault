package com.example.secretencoder.controller;

import com.example.secretencoder.dto.TextDecodeRequest;
import com.example.secretencoder.dto.TextEncodeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SteganographyControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMultipartFile createPngMockFile(String name, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(10, 25, 45));
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return new MockMultipartFile("image", name + ".png", "image/png", baos.toByteArray());
    }

    @Test
    @DisplayName("Should calculate image capacity correctly")
    void testImageCapacity() throws Exception {
        MockMultipartFile file = createPngMockFile("sample", 200, 200);

        mockMvc.perform(multipart("/api/steganography/image/capacity")
                        .file(file)
                        .param("messageLength", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.width").value(200))
                .andExpect(jsonPath("$.data.height").value(200))
                .andExpect(jsonPath("$.data.canFit").value(true))
                .andExpect(jsonPath("$.data.maxCapacityBytes").value(15000));
    }

    @Test
    @DisplayName("Should encode and decode secret message in image via REST APIs")
    void testImageEncodeAndDecodeApis() throws Exception {
        MockMultipartFile originalFile = createPngMockFile("carrier", 150, 150);
        String secret = "TopSecretRESTPayload2026";
        String password = "StrongRestPassword!1";

        // 1. Encode
        byte[] encodedPngBytes = mockMvc.perform(multipart("/api/steganography/image/encode")
                        .file(originalFile)
                        .param("message", secret)
                        .param("password", password))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // 2. Decode
        MockMultipartFile encodedFile = new MockMultipartFile(
                "image", "encoded.png", "image/png", encodedPngBytes
        );

        mockMvc.perform(multipart("/api/steganography/image/decode")
                        .file(encodedFile)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.secretMessage").value(secret));
    }

    @Test
    @DisplayName("Should encode and decode secret message in text via REST APIs")
    void testTextStegoApis() throws Exception {
        TextEncodeRequest encodeReq = new TextEncodeRequest(
                "Normal business meeting cover text.",
                "Confidential Agent Briefing",
                "AgentPassword!1"
        );

        String encodeResJson = mockMvc.perform(post("/api/steganography/text/encode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(encodeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.encodedText").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var tree = objectMapper.readTree(encodeResJson);
        String encodedCarrierText = tree.path("data").path("encodedText").asText();

        // Decode
        TextDecodeRequest decodeReq = new TextDecodeRequest(encodedCarrierText, "AgentPassword!1");

        mockMvc.perform(post("/api/steganography/text/decode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decodeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.secretMessage").value("Confidential Agent Briefing"));
    }
}
