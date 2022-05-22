package antifraud.user;

import antifraud.request.AccessRequest;
import antifraud.request.RoleRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }


    public User registerUser(User user) { // register new user and check if already exists, If yes throw exception

        if (isFound(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        } else {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            repository.save(user);

            updateRoles(user); // after saving account we can find it by Id and update it's roles
            repository.delete(user); // then we delete original account without roles
            repository.save(user); // and save same account but updated with roles

            return user;
        }

    }

    public List<User> getUserList() { // get everyone from database
        return repository.getAllByOrderByIdAsc();
    }

    public Map<String, String> deleteUser(String username) {
        Optional<User> userOptional = repository.findAccountByUsernameIgnoreCase(username);
        if (userOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        repository.delete(userOptional.get());
        return Map.of("username", username,
                        "status", "Deleted successfully!");
    }

    @Transactional
    public User changeRole(RoleRequest request) {
        Optional<User> optionalUser = repository.findAccountByUsernameIgnoreCase(request.getUsername());

        if (optionalUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        } else if (!request.getRole().equals("SUPPORT") && !request.getRole().equals("MERCHANT")) { // if we want to assign wrong role (ADMINISTRATOR) throw
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong role to change");
        }

        User userToChange = optionalUser.get();

        if (request.getRole().equals(userToChange.getRole())) { // if user has same role as the one we want to assign throw
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot assign same role to user");
        }

        userToChange.setRole(request.getRole());

       return userToChange;
    }

    @Transactional
    public Map<String, String> changeAccess(AccessRequest request) {
        Optional<User> optionalUser = repository.findAccountByUsernameIgnoreCase(request.getUsername().toLowerCase());
        if (optionalUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        if (optionalUser.get().getRole().equals("ADMINISTRATOR")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot block Administrator");
        }

        User userToChange = optionalUser.get();

        if (request.getOperation().equals("UNLOCK")) {

            userToChange.setAccountNonLocked(true);
            return Map.of("status", "User " + userToChange.getUsername() + " unlocked!");
        } else {
            userToChange.setAccountNonLocked(false);
            return Map.of("status", "User " + userToChange.getUsername() + " locked!");
        }
    }


    private boolean isFound(String username) {
        Optional<User> userOptional = repository.findAccountByUsernameIgnoreCase(username);
        return userOptional.isPresent();
    }

    private void updateRoles(User user) { // update roles while registering user
        if (user.getId() == 1) {
            user.setRole("ADMINISTRATOR");
            user.setAccountNonLocked(true);
        } else {
            user.setRole("MERCHANT");
        }
    }

}
