package com.wingsup.warehouse.controller;

import com.wingsup.warehouse.model.Lecture;
import com.wingsup.warehouse.repository.LectureRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/lectures")
public class LectureController {

    private final LectureRepository lectureRepository;

    public LectureController(LectureRepository lectureRepository) {
        this.lectureRepository = lectureRepository;
    }

    @GetMapping
    public List<Lecture> getAll() {
        return lectureRepository.findAll();
    }

    @PostMapping
    public Lecture create(@RequestBody Lecture lecture) {

        if (lecture.getName() == null
                || lecture.getName().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên bài giảng không được để trống"
            );
        }

        if (lecture.getLink() == null
                || lecture.getLink().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Link bài giảng không được để trống"
            );
        }

        String name = lecture.getName().trim();

        if (!name.matches("[\\p{L}\\p{N}\\s]+")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên bài giảng không được có ký tự đặc biệt"
            );
        }

        if (lectureRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tên bài giảng '" + name + "' đã tồn tại"
            );
        }

        lecture.setName(name);
        lecture.setLink(lecture.getLink().trim());

        return lectureRepository.save(lecture);
    }

    @PutMapping("/{id}")
    public Lecture update(
            @PathVariable Long id,
            @RequestBody Lecture data
    ) {

        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy bài giảng"
                )
                );

        if (data.getName() == null
                || data.getName().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên bài giảng không được để trống"
            );
        }

        if (data.getLink() == null
                || data.getLink().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Link bài giảng không được để trống"
            );
        }

        String name = data.getName().trim();

        if (!name.matches("[\\p{L}\\p{N}\\s]+")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên bài giảng không được có ký tự đặc biệt"
            );
        }

        boolean trungTen = lectureRepository.findAll()
                .stream()
                .anyMatch(x
                        -> !x.getId().equals(id)
                && x.getName() != null
                && x.getName().trim().equalsIgnoreCase(name)
                );

        if (trungTen) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tên bài giảng '" + name + "' đã tồn tại"
            );
        }

        lecture.setName(name);
        lecture.setLink(data.getLink().trim());

        return lectureRepository.save(lecture);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {

        if (!lectureRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy bài giảng"
            );
        }

        lectureRepository.deleteById(id);
    }
}
