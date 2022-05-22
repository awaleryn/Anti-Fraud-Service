package antifraud.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotEmpty;

@Data
@RequiredArgsConstructor
@Entity
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotEmpty
    private String number;

    @JsonIgnore
    private long allowedLimit = 200;

    @JsonIgnore
    private int manualLimit = 1500;

    @JsonIgnore
    private boolean locked;

    public Card(String number, boolean locked) {
        this.number = number;
        this.locked = locked;
    }

}
