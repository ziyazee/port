package com.porter.ksa.config;

import com.porter.ksa.entity.AccountStatus;
import com.porter.ksa.entity.CustomerAccount;
import com.porter.ksa.entity.DocumentReference;
import com.porter.ksa.repository.CustomerAccountRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class CustomerAccountSeeder implements CommandLineRunner {

    private final CustomerAccountRepository customerAccounts;

    public CustomerAccountSeeder(CustomerAccountRepository customerAccounts) {
        this.customerAccounts = customerAccounts;
    }

    @Override
    public void run(String... args) {
        if (customerAccounts.count() > 0) {
            return;
        }

        customerAccounts.saveAll(List.of(
                customer(
                        "Saudi Aramco Logistics",
                        "CR-101245",
                        "VAT-300101245",
                        "Dhahran Industrial Area, Dhahran",
                        "+966501112233",
                        "operations@aramco-logistics.sa",
                        List.of("dispatch@aramco-logistics.sa", "finance@aramco-logistics.sa"),
                        AccountStatus.VERIFIED
                ),
                customer(
                        "SABIC Supply Chain",
                        "CR-204810",
                        "VAT-300204810",
                        "Jubail Industrial City, Jubail",
                        "+966502223344",
                        "transport@sabic-sc.sa",
                        List.of("orders@sabic-sc.sa"),
                        AccountStatus.VERIFIED
                ),
                customer(
                        "Maaden Industrial Minerals",
                        "CR-309377",
                        "VAT-300309377",
                        "Ras Al Khair Industrial City",
                        "+966503334455",
                        "logistics@maaden.sa",
                        List.of("contracts@maaden.sa", "fleetdesk@maaden.sa"),
                        AccountStatus.PENDING_FOR_VERIFICATION
                ),
                customer(
                        "Red Sea Construction Co",
                        "CR-412908",
                        "VAT-300412908",
                        "King Abdullah Economic City, Jeddah",
                        "+966504445566",
                        "projects@redseaconstruction.sa",
                        List.of("siteops@redseaconstruction.sa"),
                        AccountStatus.CREATED
                ),
                customer(
                        "Riyadh Steel Works",
                        "CR-556201",
                        "VAT-300556201",
                        "Second Industrial City, Riyadh",
                        "+966505556677",
                        "dispatch@riyadhsteel.sa",
                        List.of("procurement@riyadhsteel.sa"),
                        AccountStatus.VERIFIED
                ),
                customer(
                        "Gulf Cold Chain",
                        "CR-681420",
                        "VAT-300681420",
                        "Dammam Logistics Park, Dammam",
                        "+966506667788",
                        "control@gulfcoldchain.sa",
                        List.of("nightdesk@gulfcoldchain.sa", "billing@gulfcoldchain.sa"),
                        AccountStatus.DEACTIVATED
                )
        ));
    }

    private CustomerAccount customer(
            String companyName,
            String registrationNumber,
            String vatNumber,
            String nationalAddress,
            String contactNumber,
            String email,
            List<String> commsEmails,
            AccountStatus status
    ) {
        CustomerAccount customerAccount = new CustomerAccount(
                companyName,
                registrationNumber,
                vatNumber,
                nationalAddress,
                contactNumber,
                email,
                commsEmails,
                document(registrationNumber, "registration"),
                document(registrationNumber, "vat"),
                document(registrationNumber, "national-address"),
                "system-seeder"
        );
        customerAccount.updateStatus(status, "system-seeder");
        return customerAccount;
    }

    private DocumentReference document(String registrationNumber, String documentType) {
        String filename = registrationNumber.toLowerCase() + "-" + documentType + ".pdf";
        return new DocumentReference(
                "seed/customers/" + filename,
                filename,
                "application/pdf",
                128_000
        );
    }
}
