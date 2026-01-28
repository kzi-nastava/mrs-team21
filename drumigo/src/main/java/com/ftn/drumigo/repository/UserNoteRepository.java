package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.users.User;
import com.ftn.drumigo.domain.UserNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserNoteRepository extends JpaRepository<UserNote, Long> {
    List<UserNote> findByUser(User user);
}

