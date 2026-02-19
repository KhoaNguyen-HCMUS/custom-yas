package com.yas.media.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.service.MediaService;
import com.yas.media.viewmodel.MediaPostVm;
import com.yas.media.viewmodel.MediaVm;
import com.yas.media.viewmodel.NoFileMediaVm;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

class MediaControllerTest {

    @Mock
    private MediaService mediaService;

    private MediaController mediaController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mediaController = new MediaController(mediaService);
    }

    @Test
    void create_whenValidRequest_thenReturnNoFileMediaVm() {
        MockMultipartFile multipartFile = new MockMultipartFile(
            "file",
            "example.png",
            "image/png",
            new byte[] {}
        );
        MediaPostVm postVm = new MediaPostVm("caption", multipartFile, "override.png");

        Media media = new Media();
        media.setId(1L);
        media.setCaption("caption");
        media.setFileName("override.png");
        media.setMediaType("image/png");

        when(mediaService.saveMedia(postVm)).thenReturn(media);

        ResponseEntity<Object> response = mediaController.create(postVm);

        assertEquals(200, response.getStatusCode().value());
        assertThat(response.getBody()).isInstanceOf(NoFileMediaVm.class);
        NoFileMediaVm body = (NoFileMediaVm) response.getBody();
        assertEquals(1L, body.id());
        assertEquals("caption", body.caption());
        assertEquals("override.png", body.fileName());
        assertEquals("image/png", body.mediaType());
    }

    @Test
    void delete_whenValidId_thenReturnNoContent() {
        doNothing().when(mediaService).removeMedia(1L);

        ResponseEntity<Void> response = mediaController.delete(1L);

        assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void get_whenMediaFound_thenReturnOk() {
        MediaVm vm = new MediaVm(1L, "caption", "file", "image/png", "/url");
        when(mediaService.getMediaById(1L)).thenReturn(vm);

        ResponseEntity<MediaVm> response = mediaController.get(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(vm, response.getBody());
    }

    @Test
    void get_whenMediaNotFound_thenReturnNotFound() {
        when(mediaService.getMediaById(1L)).thenReturn(null);

        ResponseEntity<MediaVm> response = mediaController.get(1L);

        assertEquals(404, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    void getByIds_whenMediasFound_thenReturnOk() {
        MediaVm vm = new MediaVm(1L, "caption", "file", "image/png", "/url");
        when(mediaService.getMediaByIds(List.of(1L, 2L))).thenReturn(List.of(vm));

        ResponseEntity<List<MediaVm>> response = mediaController.getByIds(List.of(1L, 2L));

        assertEquals(200, response.getStatusCode().value());
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getByIds_whenEmptyList_thenReturnNotFound() {
        when(mediaService.getMediaByIds(any())).thenReturn(List.of());

        ResponseEntity<List<MediaVm>> response = mediaController.getByIds(List.of(1L, 2L));

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getFile_whenValidRequest_thenReturnResource() {
        byte[] content = "data".getBytes();
        InputStream is = new ByteArrayInputStream(content);
        MediaDto dto = MediaDto.builder()
            .content(is)
            .mediaType(MediaType.IMAGE_PNG)
            .build();

        when(mediaService.getFile(1L, "file.png")).thenReturn(dto);

        ResponseEntity<InputStreamResource> response = mediaController.getFile(1L, "file.png");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("attachment; filename=\"file.png\"",
            response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
        assertThat(response.getBody()).isNotNull();
    }
}

