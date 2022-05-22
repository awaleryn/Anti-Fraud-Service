package antifraud.authentication;

import antifraud.request.AccessRequest;
import antifraud.request.RoleRequest;
import antifraud.user.User;
import antifraud.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/auth/")
@Validated
public class AuthenticationRestController {

    private final UserService userService;

    @Autowired
    public AuthenticationRestController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("user")
    @ResponseStatus(HttpStatus.CREATED)
    public User registerUser(@RequestBody @Valid User user) {
        if (StringUtils.isEmpty(user.getUsername()) ||
            StringUtils.isEmpty(user.getName()) ||
            StringUtils.isEmpty(user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return userService.registerUser(user);
    }

    @GetMapping("list")
    @ResponseStatus(HttpStatus.OK)
    public List<User> getUserList() {
        return userService.getUserList();
    }

    @DeleteMapping("user/{username}")
    public Map<String, String> deleteUser(@PathVariable String username) {
        return userService.deleteUser(username);
    }

    @PutMapping("role")
    public User changeRole(@RequestBody RoleRequest request) {
        return userService.changeRole(request);
    }

    @PutMapping("access")
    public Map<String, String> changeAccess(@RequestBody @Valid AccessRequest request) {
        return userService.changeAccess(request);
    }

}
