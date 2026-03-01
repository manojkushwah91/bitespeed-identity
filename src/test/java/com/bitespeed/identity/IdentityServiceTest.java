package com.bitespeed.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class IdentityServiceTest {

    @Autowired
    private IdentityService service;

    @Test
    void createsPrimaryContactWhenNoMatchExists() {
        IdentifyRequest req = new IdentifyRequest();
        req.setEmail("a@b.com");
        req.setPhoneNumber("111");

        IdentifyResponse res = service.identify(req);

        assertThat(res.getContact().getPrimaryContactId()).isNotNull();
        assertThat(res.getContact().getEmails()).containsExactly("a@b.com");
        assertThat(res.getContact().getPhoneNumbers()).containsExactly("111");
        assertThat(res.getContact().getSecondaryContactIds()).isEmpty();
    }

    @Test
    void linksSecondaryWhenEmailAlreadyExists() {
        IdentifyRequest first = new IdentifyRequest();
        first.setEmail("x@y.com");
        first.setPhoneNumber("123");

        IdentifyRequest second = new IdentifyRequest();
        second.setEmail("x@y.com");
        second.setPhoneNumber("456");

        IdentifyResponse r1 = service.identify(first);
        IdentifyResponse r2 = service.identify(second);

        assertThat(r2.getContact().getPrimaryContactId())
            .isEqualTo(r1.getContact().getPrimaryContactId());

        assertThat(r2.getContact().getPhoneNumbers())
            .containsExactlyInAnyOrder("123", "456");

        assertThat(r2.getContact().getSecondaryContactIds()).hasSize(1);
    }

    @Test
    void oldestPrimaryWinsWhenTwoPrimariesExist() {
        IdentifyRequest r1 = new IdentifyRequest();
        r1.setEmail("old@a.com");
        r1.setPhoneNumber("111");

        IdentifyRequest r2 = new IdentifyRequest();
        r2.setEmail("new@a.com");
        r2.setPhoneNumber("111");

        IdentifyResponse res1 = service.identify(r1);
        IdentifyResponse res2 = service.identify(r2);

        assertThat(res2.getContact().getPrimaryContactId())
            .isEqualTo(res1.getContact().getPrimaryContactId());

        assertThat(res2.getContact().getEmails())
            .contains("old@a.com", "new@a.com");
    }
}
