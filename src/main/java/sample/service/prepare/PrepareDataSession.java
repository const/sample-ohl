package sample.service.prepare;

import com.fasterxml.uuid.Generators;
import net.datafaker.Faker;
import net.datafaker.providers.base.Company;
import sample.dto.*;
import sample.model.ContactType;
import sample.model.VerificationStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PrepareDataSession {
    private final static List<ContactType> contactTypes = List.of(ContactType.values());
    public static final int MAX_OFFICES = 3;
    public static final int MAX_CONTACTS = 4;
    private final Faker faker = new Faker();
    private final Random rnd = new Random();
    private int count;

    public PrepareDataSession(int current) {
        count = current;
    }

    public List<CompanyDto> prepareBatch(int batchSize) {
        return IntStream.range(0, batchSize).mapToObj(i -> prepareEntity()).toList();
    }

    UUID uuid() {
        return Generators.timeBasedEpochGenerator().generate();
    }

    private CompanyDto prepareEntity() {
        // Note, datafaker creates too many duplicates, so unique suffixes are added
        Company c = faker.company();
        var company = new CompanyDto();
        company.setId(uuid());
        company.setName(c.name() + " R" + (++count));
        company.setUrl(faker.internet().url());
        company.setIndustry(c.industry());
        company.setDescription(c.catchPhrase());
        if (rnd.nextInt(10) == 0) {
            var verification = new VerificationInfoDto();
            verification.setTimestamp(faker.timeAndDate().past(3000, TimeUnit.DAYS));
            verification.setStatus(rnd.nextInt(10) == 0 ? VerificationStatus.INVALID : VerificationStatus.VERIFIED);
            verification.setUser(faker.internet().username());
            verification.setComment(rnd.nextInt(100) == 0 ? faker.text().text(200) : null);
            company.setVerified(verification);
        }
        int offices = 1 + rnd.nextInt(MAX_OFFICES);
        company.setOffices(new ArrayList<>());
        for (int i = 0; i < offices; i++) {
            var office = new OfficeDto();
            office.setId(uuid());
            office.setName(faker.funnyName().name());
            var a = faker.address();
            office.setCity(a.city());
            office.setAddress(a.fullAddress());
            company.getOffices().add(office);
        }
        int contacts = 1 + rnd.nextInt(MAX_CONTACTS);
        company.setContactPersons(new ArrayList<>());
        for (int i = 0; i < contacts; i++) {
            var contact = new ContactPersonDto();
            contact.setId(uuid());
            contact.setPosition(faker.job().position());
            var n = faker.name();
            contact.setName(n.fullName());
            var details = faker.options().subset(
                    1 + rnd.nextInt(contactTypes.size()),
                    ContactType.values());
            final String username = faker.internet().username();
            contact.setDetails(details.stream().map(t -> {
                var d = new ContactDetailsDto();
                d.setType(t);
                d.setValue(switch (t) {
                    case PHONE -> faker.phoneNumber().cellPhone();
                    case EMAIL -> faker.internet().safeEmailAddress(username + count);
                    case TELEGRAM -> "@" + username + count;
                });
                return d;
            }).collect(Collectors.toList()));
            company.getContactPersons().add(contact);
        }
        return company;
    }
}
