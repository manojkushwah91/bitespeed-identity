package com.bitespeed.identity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class IdentityService {

    private final ContactRepository repo;

    public IdentityService(ContactRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public IdentifyResponse identify(IdentifyRequest req) {

        if (req.getEmail() == null && req.getPhoneNumber() == null) {
            throw new IllegalArgumentException("Email or phoneNumber required");
        }

        List<Contact> matches =
                repo.findActiveByEmailOrPhone(req.getEmail(), req.getPhoneNumber());

        // Case 1: No match → create primary
        if (matches.isEmpty()) {
            Contact primary = new Contact();
            primary.setEmail(req.getEmail());
            primary.setPhoneNumber(req.getPhoneNumber());
            primary.setLinkPrecedence(Contact.LinkPrecedence.primary);
            repo.save(primary);
            return buildResponse(primary, List.of());
        }

        // ✅ Collect ALL primaries safely
        List<Contact> primaries = matches.stream()
                .filter(c -> c.getLinkPrecedence() == Contact.LinkPrecedence.primary)
                .collect(Collectors.toList());

        Contact rootPrimary = primaries.stream()
                .min(Comparator.comparing(Contact::getCreatedAt))
                .orElseThrow();

        // Fetch full cluster
        List<Contact> cluster =
                repo.findByIdOrLinkedId(rootPrimary.getId(), rootPrimary.getId());

        // Normalize other primaries
        for (Contact c : cluster) {
            if (c.getLinkPrecedence() == Contact.LinkPrecedence.primary &&
                !c.getId().equals(rootPrimary.getId())) {

                c.setLinkPrecedence(Contact.LinkPrecedence.secondary);
                c.setLinkedId(rootPrimary.getId());
                repo.save(c);
            }
        }

        boolean emailNew = req.getEmail() != null &&
                cluster.stream().noneMatch(c -> req.getEmail().equals(c.getEmail()));

        boolean phoneNew = req.getPhoneNumber() != null &&
                cluster.stream().noneMatch(c -> req.getPhoneNumber().equals(c.getPhoneNumber()));

        // Create secondary ONLY if something is new
        if (emailNew || phoneNew) {
            Contact secondary = new Contact();
            secondary.setEmail(req.getEmail());
            secondary.setPhoneNumber(req.getPhoneNumber());
            secondary.setLinkedId(rootPrimary.getId());
            secondary.setLinkPrecedence(Contact.LinkPrecedence.secondary);
            repo.save(secondary);
            cluster.add(secondary);
        }

        return buildResponse(rootPrimary, cluster);
    }

    private IdentifyResponse buildResponse(Contact primary, List<Contact> cluster) {

        List<String> emails = new ArrayList<>();
        List<String> phones = new ArrayList<>();
        List<Integer> secondaryIds = new ArrayList<>();

        if (primary.getEmail() != null) emails.add(primary.getEmail());
        if (primary.getPhoneNumber() != null) phones.add(primary.getPhoneNumber());

        for (Contact c : cluster) {
            if (!c.getId().equals(primary.getId())) {
                if (c.getEmail() != null && !emails.contains(c.getEmail()))
                    emails.add(c.getEmail());
                if (c.getPhoneNumber() != null && !phones.contains(c.getPhoneNumber()))
                    phones.add(c.getPhoneNumber());
                if (c.getLinkPrecedence() == Contact.LinkPrecedence.secondary)
                    secondaryIds.add(c.getId());
            }
        }

        IdentifyResponse.ContactPayload payload =
                new IdentifyResponse.ContactPayload();
        payload.setPrimaryContactId(primary.getId());
        payload.setEmails(emails);
        payload.setPhoneNumbers(phones);
        payload.setSecondaryContactIds(secondaryIds);

        IdentifyResponse res = new IdentifyResponse();
        res.setContact(payload);
        return res;
    }
}