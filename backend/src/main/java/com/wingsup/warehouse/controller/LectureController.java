/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.Lecture;
import com.wingsup.warehouse.repository.LectureRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lectures")
public class LectureController {

    private final LectureRepository lectureRepository;

    public LectureController(LectureRepository lectureRepository) {
        this.lectureRepository = lectureRepository;
    }

    // Lấy danh sách bài giảng
    @GetMapping
    public List<Lecture> getAll() {
        return lectureRepository.findAll();
    }

    // Thêm bài giảng
    @PostMapping
    public Lecture create(@RequestBody Lecture lecture) {

        if (lecture.getName() == null || lecture.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên bài giảng không được để trống");
        }

        if (lecture.getLink() == null || lecture.getLink().trim().isEmpty()) {
            throw new RuntimeException("Link bài giảng không được để trống");
        }

        lecture.setName(lecture.getName().trim());
        lecture.setLink(lecture.getLink().trim());

        return lectureRepository.save(lecture);
    }

    // Sửa bài giảng
    @PutMapping("/{id}")
    public ResponseEntity<Lecture> update(
            @PathVariable Long id,
            @RequestBody Lecture data
    ) {

        return lectureRepository.findById(id)
                .map(lecture -> {

                    lecture.setName(data.getName().trim());
                    lecture.setLink(data.getLink().trim());

                    return ResponseEntity.ok(
                            lectureRepository.save(lecture)
                    );
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Xóa bài giảng
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        if (!lectureRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        lectureRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
