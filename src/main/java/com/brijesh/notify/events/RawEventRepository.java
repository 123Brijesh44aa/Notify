package com.brijesh.notify.events;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawEventRepository extends JpaRepository<RawEvent, Long> {


}
